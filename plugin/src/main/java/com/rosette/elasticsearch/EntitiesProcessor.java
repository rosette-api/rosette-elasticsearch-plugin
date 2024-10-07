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

import com.basistech.util.LanguageCode;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntitiesProcessor extends RosetteAbstractProcessor {
    public static final String TYPE = "ros_entities";
    private static final String SERVICE_PATH = "entities";
    private static final Logger LOGGER = LogManager.getLogger(TYPE);
    private final boolean includeOffsets;
    private final boolean doTranslate;
    private final LanguageCode translateLanguage;
    private final boolean doSentiment;

    EntitiesProcessor(RosetteApiWrapper rosAPI, String tag, String description, String inputField, String targetField,
                      boolean includeOffsets, boolean doTranslate, LanguageCode translateLanguage,
                      boolean doSentiment) {
        super(rosAPI, tag, description, TYPE, inputField, targetField);
        this.includeOffsets = includeOffsets;
        this.doTranslate = doTranslate;
        this.translateLanguage = translateLanguage;
        this.doSentiment = doSentiment;
        LOGGER.info("Creating EntitiesProcessor {}", TYPE);
    }

    @Override
    public void processDocument(String inputText, IngestDocument ingestDocument) throws ElasticsearchException {
        JsonNode resp;
        //If entity level sentiment is desired, use the entity information from the ASCENT call
        try {
            //SENTIMENT
            if (doSentiment) {
                resp = rosAPI.performDocumentRequest(SentimentProcessor.SERVICE_PATH, inputText, null);
            } else {
                resp = rosAPI.performDocumentRequest(SERVICE_PATH, inputText, null);
            }
        } catch (HttpClientException | HttpServerException ex) {
            LOGGER.error(ex.getMessage());
            throw new ElasticsearchException(ex.getMessage(), ex);
        }
        JsonNode elems = resp.get("entities");
        List<Map<String, Object>> entityList = new ArrayList<>();
        if (elems != null) {
            for (JsonNode elem : elems) {
                entityList.add(processEntity(elem));
            }
        }
        ingestDocument.setFieldValue(targetField, entityList);
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
            boolean includeOffsets = ConfigurationUtils.readBooleanProperty(TYPE, processorTag, config,
                    Parameters.OFFSETS.name, Boolean.parseBoolean(Parameters.OFFSETS.defaultValue));
            boolean doTranslate = ConfigurationUtils.readBooleanProperty(TYPE, processorTag, config,
                    Parameters.TRANSLATE.name, Boolean.parseBoolean(Parameters.TRANSLATE.defaultValue));
            LanguageCode translateLanguage = LanguageCode.lookupByISO639(ConfigurationUtils
                    .readStringProperty(TYPE, processorTag, config,
                            Parameters.TRANSLATE_LANGUAGE.name, Parameters.TRANSLATE_LANGUAGE.defaultValue));
            boolean doSentiment = ConfigurationUtils.readBooleanProperty(TYPE, processorTag, config,
                    Parameters.SENTIMENT.name, Boolean.parseBoolean(Parameters.SENTIMENT.defaultValue));

            return new EntitiesProcessor(rosAPI, processorTag, processorDescription, inputField, targetField,
                    includeOffsets, doTranslate, translateLanguage, doSentiment);

        }
    }

    private HashMap<String, Object> processEntity(JsonNode entity) {
        HashMap<String, Object> toReturn = new HashMap<>();
        String type = entity.get("type").asText();
        String normalized = entity.get("normalized").asText();
        toReturn.put("mention", normalized);
        toReturn.put("entityId", entity.get("entityId").asText());
        toReturn.put("type", type);
        toReturn.put("count", entity.get("count").asInt());

        if (includeOffsets) {
            List<Map<String, Object>> offsets = new ArrayList<>();
            if (entity.get("mentionOffsets") != null) {
                for (JsonNode mention : entity.get("mentionOffsets")) {
                    Map<String, Object> offsetMap = new HashMap<>();
                    offsetMap.put("start", mention.get("startOffset").asInt());
                    offsetMap.put("end", mention.get("endOffset").asInt());
                    offsets.add(offsetMap);
                }
            }
            toReturn.put("offsets", offsets);
        }
        if (doSentiment) {
            JsonNode sentiment = entity.get("sentiment");
            if (sentiment != null) {
                toReturn.put("sentiment", sentiment.get("label").asText());
            }
        }
        if (doTranslate
                && ("PERSON".equalsIgnoreCase(type)
                || "LOCATION".equalsIgnoreCase(type)
                || "ORGANIZATION".equalsIgnoreCase(type))) {

            try {
                JsonNode rntresponse = rosAPI.performNameTranslationRequest(NameTranslationProcessor.SERVICE_PATH,
                        normalized, translateLanguage, type, null, null, null, null);
                toReturn.put("translation", rntresponse.get("translation").asText());
            } catch (HttpClientException | HttpServerException ex) {
                LOGGER.error(ex.getMessage());
                throw new ElasticsearchException(ex.getMessage(), ex);
            }
        }
        return toReturn;
    }

    enum Parameters {
        TARGET_FIELD("target_field", TYPE),
        OFFSETS("include_offsets", "false"),
        TRANSLATE("include_translation", "false"),
        TRANSLATE_LANGUAGE("translation_language", "eng"),
        SENTIMENT("include_sentiment", "false");

        String name;
        String defaultValue;

        Parameters(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }
    }
}

