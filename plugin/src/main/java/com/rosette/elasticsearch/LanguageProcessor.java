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
import com.basistech.rosette.apimodel.LanguageOptions;
import com.basistech.rosette.apimodel.LanguageResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

public class LanguageProcessor extends RosetteAbstractProcessor {
    public static final String LANGUAGE_SERVICE_PATH = "/language";
    public static final String TYPE = "ros_language";
    private static final Logger LOGGER = LogManager.getLogger(LanguageProcessor.class);

    LanguageProcessor(RosetteApiWrapper rosAPI, String tag, String description, String inputField, String targetField) {
        super(rosAPI, tag, description, TYPE, inputField, targetField);
    }

    @Override
    public void processDocument(String inputText, IngestDocument ingestDocument) throws Exception {
        // call /language endpoint and set the result in the field
        DocumentRequest<LanguageOptions> request = DocumentRequest.<LanguageOptions>builder()
                .content(inputText).build();
        LanguageResponse response;
        try {
            // RosApi client binding's Jackson needs elevated privilege
            response = AccessController.doPrivileged((PrivilegedAction<LanguageResponse>) () ->
                    rosAPI.getHttpRosetteAPI().perform(LANGUAGE_SERVICE_PATH, request,
                            LanguageResponse.class)
            );
        } catch (HttpRosetteAPIException ex) {
            LOGGER.error(ex.getErrorResponse().getMessage());
            throw new ElasticsearchException(ex.getErrorResponse().getMessage(), ex);
        }

        if (response.getLanguageDetections() != null
                && !response.getLanguageDetections().isEmpty()
                && response.getLanguageDetections().get(0) != null
                && response.getLanguageDetections().get(0).getLanguage() != null) {
            ingestDocument.setFieldValue(targetField, response.getLanguageDetections().get(0).getLanguage().ISO639_3());
        } else {
            throw new ElasticsearchException(TYPE + " ingest processor failed to guess language of document.");
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
            return new LanguageProcessor(rosAPI, processorTag, processorDescription, inputField, targetField);
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
