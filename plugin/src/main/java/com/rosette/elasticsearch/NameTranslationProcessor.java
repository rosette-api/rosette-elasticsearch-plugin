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

import com.basistech.rosette.api.HttpRosetteAPIException;
import com.basistech.rosette.api.common.AbstractRosetteAPI;
import com.basistech.rosette.apimodel.NameTranslationRequest;
import com.basistech.rosette.apimodel.NameTranslationResponse;
import com.basistech.util.ISO15924;
import com.basistech.util.LanguageCode;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

public class NameTranslationProcessor extends RosetteAbstractProcessor {

    public static final String TYPE = "ros_name_translation";

    private static final Logger LOGGER = ESLoggerFactory.getLogger(NameTranslationProcessor.class.getName());

    private LanguageCode targetLanguage;
    private ISO15924 targetScript;
    private String entityType;
    private LanguageCode sourceLanguage;
    private ISO15924 sourceScript;
    private LanguageCode sourceOrigin;

    NameTranslationProcessor(RosetteApiWrapper rosAPI, String tag, String inputField, String targetField,
                             LanguageCode targetLanguage, ISO15924 targetScript, String entityType,
                             LanguageCode sourceLanguage, ISO15924 sourceScript, LanguageCode sourceOrigin) {
        super(rosAPI, tag, TYPE, inputField, targetField);
        this.targetLanguage = targetLanguage;
        this.targetScript = targetScript;
        this.entityType = entityType;
        this.sourceLanguage = sourceLanguage;
        this.sourceScript = sourceScript;
        this.sourceOrigin = sourceOrigin;
    }

    @Override
    public void processDocument(String inputText, IngestDocument ingestDocument) throws Exception {
        // call /name-translation endpoint and set the result in the field
        NameTranslationRequest request = new NameTranslationRequest.Builder(inputText, targetLanguage)
                .entityType(entityType)
                .targetScript(targetScript)
                .sourceLanguageOfUse(sourceLanguage)
                .sourceLanguageOfOrigin(sourceOrigin)
                .sourceScript(sourceScript).build();

        NameTranslationResponse response;
        try {
            // RosApi client binding's Jackson needs elevated privilege
            response = AccessController.doPrivileged((PrivilegedAction<NameTranslationResponse>) () ->
                    rosAPI.getHttpRosetteAPI().perform(AbstractRosetteAPI.NAME_TRANSLATION_SERVICE_PATH, request, NameTranslationResponse.class)
            );
        } catch (HttpRosetteAPIException ex) {
            LOGGER.error(ex.getErrorResponse().getMessage());
            throw new ElasticsearchException(ex.getErrorResponse().getMessage(), ex);
        }

        ingestDocument.setFieldValue(targetField, response.getTranslation());
    }

    public static final class Factory implements Processor.Factory {
        private RosetteApiWrapper rosAPI;

        Factory(RosetteApiWrapper rosAPI) {
            this.rosAPI = rosAPI;
        }

        @Override
        public Processor create(Map<String, Processor.Factory> registry, String processorTag, Map<String, Object> config) throws Exception {
            String inputField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "field");
            String targetField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, Parameters.TARGET_FIELD.name, Parameters.TARGET_FIELD.defaultValue);
            LanguageCode targetLanguage = LanguageCode.lookupByISO639(ConfigurationUtils.readStringProperty(TYPE, processorTag, config, Parameters.TARGET_LANGUAGE.name, Parameters.TARGET_LANGUAGE.defaultValue));
            ISO15924 targetScript = ISO15924.lookupByCode4(ConfigurationUtils.readStringProperty(TYPE, processorTag, config, Parameters.TARGET_SCRIPT.name, Parameters.TARGET_SCRIPT.defaultValue));
            String entityType = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, Parameters.ENTITY_TYPE.name, Parameters.ENTITY_TYPE.defaultValue);

            LanguageCode sourceLanguage = LanguageCode.lookupByISO639(ConfigurationUtils.readStringProperty(TYPE, processorTag, config, Parameters.SOURCE_LANGUAGE.name, Parameters.SOURCE_LANGUAGE.defaultValue));
            ISO15924 sourceScript = ISO15924.lookupByCode4(ConfigurationUtils.readStringProperty(TYPE, processorTag, config, Parameters.SOURCE_SCRIPT.name, Parameters.SOURCE_SCRIPT.defaultValue));
            LanguageCode sourceOrigin = LanguageCode.lookupByISO639(ConfigurationUtils.readStringProperty(TYPE, processorTag, config, Parameters.SOURCE_LANGUAGE_ORIGIN.name, Parameters.SOURCE_LANGUAGE_ORIGIN.defaultValue));

            return new NameTranslationProcessor(rosAPI, processorTag, inputField, targetField, targetLanguage, targetScript, entityType, sourceLanguage, sourceScript, sourceOrigin);
        }
    }

    enum Parameters {
        TARGET_FIELD("target_field", "ros_translation"),
        TARGET_LANGUAGE("target_language", "eng"),
        TARGET_SCRIPT("target_script", "Zyyy"),
        ENTITY_TYPE("entity_type", "PERSON"),
        SOURCE_LANGUAGE("source_language", "xxx"),
        SOURCE_SCRIPT("source_script", "Zyyy"),
        SOURCE_LANGUAGE_ORIGIN("source_language_of_origin", "xxx");

        String name;
        String defaultValue;

        Parameters(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }
    }
}
