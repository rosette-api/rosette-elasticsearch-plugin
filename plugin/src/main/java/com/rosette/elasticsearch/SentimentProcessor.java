/*******************************************************************************
 * This data and information is proprietary to, and a valuable trade secret
 * of, Basis Technology Corp.  It is given in confidence by Basis Technology
 * and may only be used as permitted under the license agreement under which
 * it has been distributed, and in no other way.
 *
 * Copyright (c) 2024 Basis Technology Corporation All rights reserved.
 *
 * The technical data and information provided herein are provided with
 * `limited rights', and the computer software provided herein is provided
 * with `restricted rights' as those terms are defined in DAR and ASPR
 * 7-104.9(a).
 *
 ******************************************************************************/
package com.rosette.elasticsearch;

import com.basistech.rosette.api.HttpRosetteAPIException;
import com.basistech.rosette.apimodel.DocumentRequest;
import com.basistech.rosette.apimodel.SentimentOptions;
import com.basistech.rosette.apimodel.SentimentResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import static com.basistech.rosette.api.common.AbstractRosetteAPI.SENTIMENT_SERVICE_PATH;

public class SentimentProcessor extends RosetteAbstractProcessor {
    public static final String TYPE = "ros_sentiment";
    public static final String SERVICE_PATH = "sentiment";
    private static final Logger LOGGER = LogManager.getLogger(TYPE);

    SentimentProcessor(RosetteApiWrapper rosAPI, String tag, String description, String inputField,
                       String targetField) {
        super(rosAPI, tag, description, TYPE, inputField, targetField);
    }

    @Override
    public void processDocument(String inputText, IngestDocument ingestDocument) throws Exception {
        // call /sentiment endpoint and set the top result in the field
        DocumentRequest<SentimentOptions> request = DocumentRequest.<SentimentOptions>builder()
                .content(inputText).build();
        SentimentResponse response;
        try {
            // RosApi client binding's Jackson needs elevated privilege
            response = AccessController.doPrivileged((PrivilegedAction<SentimentResponse>) () ->
                    rosAPI.getHttpRosetteAPI().perform(SENTIMENT_SERVICE_PATH, request,
                            SentimentResponse.class)
            );
        } catch (HttpRosetteAPIException ex) {
            LOGGER.error(ex.getErrorResponse().getMessage());
            throw new ElasticsearchException(ex.getErrorResponse().getMessage(), ex);
        }

        if (response.getDocument() != null
                && !Strings.isNullOrEmpty(response.getDocument().getLabel())) {
            ingestDocument.setFieldValue(targetField, response.getDocument().getLabel());
        } else {
            throw new ElasticsearchException(TYPE + " ingest processor failed to determine sentiment of document.");
        }
    }

    public static final class Factory implements Processor.Factory {
        private final RosetteApiWrapper rosAPI;

        Factory(RosetteApiWrapper rosAPI) {
            this.rosAPI = rosAPI;
        }

        @Override
        public Processor create(Map<String, Processor.Factory> registry, String processorTag,
                                String processorDescription, Map<String, Object> config) {
            String inputField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "field");
            String targetField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config,
                    Parameters.TARGET_FIELD.name, Parameters.TARGET_FIELD.defaultValue);
            return new SentimentProcessor(rosAPI, processorTag, processorDescription, inputField, targetField);
        }
    }

    enum Parameters {
        TARGET_FIELD("target_field", TYPE);

        final String name;
        final String defaultValue;

        Parameters(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }
    }
}
