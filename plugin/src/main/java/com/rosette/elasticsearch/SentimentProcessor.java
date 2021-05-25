/*
* Copyright 2017 Basis Technology Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.rosette.elasticsearch;

import com.basistech.rosette.api.HttpRosetteAPI;
import com.basistech.rosette.api.HttpRosetteAPIException;
import com.basistech.rosette.apimodel.DocumentRequest;
import com.basistech.rosette.apimodel.SentimentOptions;
import com.basistech.rosette.apimodel.SentimentResponse;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import static com.basistech.rosette.api.common.AbstractRosetteAPI.SENTIMENT_SERVICE_PATH;

public class SentimentProcessor extends RosetteAbstractProcessor {

    public static final String TYPE = "ros_sentiment";

    private static final Logger LOGGER = Loggers.getLogger(SentimentProcessor.class,
            SentimentProcessor.class.getName());

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
        private RosetteApiWrapper rosAPI;

        Factory(RosetteApiWrapper rosAPI) {
            this.rosAPI = rosAPI;
        }

        @Override
        public Processor create(Map<String, Processor.Factory> registry, String processorTag,
                                String processorDescription, Map<String, Object> config) throws Exception {
            String inputField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "field");
            String targetField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config,
                    Parameters.TARGET_FIELD.name, Parameters.TARGET_FIELD.defaultValue);
            return new SentimentProcessor(rosAPI, processorTag, processorDescription, inputField, targetField);
        }
    }

    enum Parameters {
        TARGET_FIELD("target_field", "ros_sentiment");

        String name;
        String defaultValue;

        Parameters(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }
    }
}
