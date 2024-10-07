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

import com.basistech.util.ISO15924;
import com.basistech.util.LanguageCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.util.Map;

public class NameTranslationProcessor extends RosetteAbstractProcessor {
    public static final String TYPE = "ros_name_translation";
    public static final String SERVICE_PATH = "name-translation";
    private static final Logger LOGGER = LogManager.getLogger(TYPE);

    private final LanguageCode targetLanguage;
    private final ISO15924 targetScript;
    private final String entityType;
    private final LanguageCode sourceLanguage;
    private final ISO15924 sourceScript;
    private final LanguageCode sourceOrigin;

    NameTranslationProcessor(RosetteApiWrapper rosAPI, String tag, String description, String inputField,
                             String targetField, LanguageCode targetLanguage, ISO15924 targetScript, String entityType,
                             LanguageCode sourceLanguage, ISO15924 sourceScript, LanguageCode sourceOrigin) {
        super(rosAPI, tag, description, TYPE, inputField, targetField);
        this.targetLanguage = targetLanguage;
        this.targetScript = targetScript;
        this.entityType = entityType;
        this.sourceLanguage = sourceLanguage;
        this.sourceScript = sourceScript;
        this.sourceOrigin = sourceOrigin;
    }

    @Override
    public void processDocument(String inputText, IngestDocument ingestDocument) {
        // call /name-translation endpoint and set the result in the field
        try {
            JsonNode resp = rosAPI.performNameTranslationRequest(SERVICE_PATH, inputText,
                    targetLanguage, entityType, targetScript, sourceLanguage, sourceOrigin, sourceScript);
            ingestDocument.setFieldValue(targetField, resp.get("translation").asText());
        } catch (HttpClientException | HttpServerException ex) {
            LOGGER.error(ex.getMessage());
            throw new ElasticsearchException(ex.getMessage(), ex);
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
            LanguageCode targetLanguage = LanguageCode.lookupByISO639(ConfigurationUtils
                    .readStringProperty(TYPE, processorTag, config,
                            Parameters.TARGET_LANGUAGE.name, Parameters.TARGET_LANGUAGE.defaultValue));
            ISO15924 targetScript = ISO15924.lookupByCode4(ConfigurationUtils
                    .readStringProperty(TYPE, processorTag, config,
                            Parameters.TARGET_SCRIPT.name, Parameters.TARGET_SCRIPT.defaultValue));
            String entityType = ConfigurationUtils.readStringProperty(TYPE, processorTag, config,
                    Parameters.ENTITY_TYPE.name, Parameters.ENTITY_TYPE.defaultValue);

            LanguageCode sourceLanguage = LanguageCode.lookupByISO639(ConfigurationUtils
                    .readStringProperty(TYPE, processorTag, config,
                            Parameters.SOURCE_LANGUAGE.name, Parameters.SOURCE_LANGUAGE.defaultValue));
            ISO15924 sourceScript = ISO15924.lookupByCode4(ConfigurationUtils
                    .readStringProperty(TYPE, processorTag, config,
                            Parameters.SOURCE_SCRIPT.name, Parameters.SOURCE_SCRIPT.defaultValue));
            LanguageCode sourceOrigin = LanguageCode.lookupByISO639(ConfigurationUtils
                    .readStringProperty(TYPE, processorTag, config,
                            Parameters.SOURCE_LANGUAGE_ORIGIN.name, Parameters.SOURCE_LANGUAGE_ORIGIN.defaultValue));

            return new NameTranslationProcessor(rosAPI, processorTag, processorDescription, inputField, targetField,
                    targetLanguage, targetScript, entityType, sourceLanguage, sourceScript, sourceOrigin);
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

        final String name;
        final String defaultValue;

        Parameters(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }
    }
}
