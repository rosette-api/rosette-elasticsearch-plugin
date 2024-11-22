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
import com.basistech.rosette.apimodel.EntitiesOptions;
import com.basistech.rosette.apimodel.NameTranslationRequest;
import com.basistech.rosette.apimodel.NameTranslationResponse;
import com.basistech.rosette.apimodel.SentimentOptions;
import com.basistech.rosette.dm.AnnotatedText;
import com.basistech.rosette.dm.Entity;
import com.basistech.rosette.dm.Mention;
import com.basistech.util.LanguageCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.Processor;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.basistech.rosette.api.common.AbstractRosetteAPI.ENTITIES_SERVICE_PATH;
import static com.basistech.rosette.api.common.AbstractRosetteAPI.NAME_TRANSLATION_SERVICE_PATH;
import static com.basistech.rosette.api.common.AbstractRosetteAPI.SENTIMENT_SERVICE_PATH;

public class EntitiesProcessor extends RosetteAbstractProcessor {
    public static final String TYPE = "ros_entities";
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
        //Need to use the ADM for entities so we get offsets
        AnnotatedText adm;

        //If entity level sentiment is desired, use the entity information from the ASCENT call
        try {
            //SENTIMENT
            if (doSentiment) {
                DocumentRequest<SentimentOptions> sentrequest = DocumentRequest.<SentimentOptions>builder()
                        .content(inputText).build();
                adm = AccessController.doPrivileged((PrivilegedAction<AnnotatedText>) () ->
                        rosAPI.getHttpRosetteAPI().perform(SENTIMENT_SERVICE_PATH, sentrequest)
                );
            } else {
                //REX
                DocumentRequest<EntitiesOptions> entityrequest = DocumentRequest.<EntitiesOptions>builder()
                        .content(inputText).build();
                adm = AccessController.doPrivileged((PrivilegedAction<AnnotatedText>) () ->
                        rosAPI.getHttpRosetteAPI().perform(ENTITIES_SERVICE_PATH, entityrequest)
                );
            }
        } catch (HttpRosetteAPIException ex) {
            LOGGER.error(ex.getErrorResponse().getMessage());
            throw new ElasticsearchException(ex.getErrorResponse().getMessage(), ex);
        }

        List<Map<String, Object>> entities = adm.getEntities().stream().map(this::processEntity)
                .collect(Collectors.toList());

        ingestDocument.setFieldValue(targetField, entities);
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

    //Transforms the Entity object into a Map that ES can serialize.
    // Calls RNT on each head mention for a translation (if requested)
    private HashMap<String, Object> processEntity(Entity entity) {
        List<Mention> mentions = entity.getMentions();
        String headMention = mentions.get(entity.getHeadMentionIndex()).getNormalized();
        String type = entity.getType();

        HashMap<String, Object> toReturn = new HashMap<>();

        toReturn.put("mention", headMention);
        toReturn.put("entityId", entity.getEntityId());
        toReturn.put("type", type);
        toReturn.put("count", mentions.size());

        if (includeOffsets) {
            List<Map<String, Object>> offsets = new ArrayList<>();
            for (Mention mention : mentions) {
                Map<String, Object> offsetMap = new HashMap<>();
                offsetMap.put("start", mention.getStartOffset());
                offsetMap.put("end", mention.getEndOffset());
                offsets.add(offsetMap);
            }
            toReturn.put("offsets", offsets);
        }

        //RNT
        if (doTranslate
                && ("PERSON".equalsIgnoreCase(type)
                || "LOCATION".equalsIgnoreCase(type)
                || "ORGANIZATION".equalsIgnoreCase(type))) {
            NameTranslationRequest rntrequest = NameTranslationRequest.builder()
                    .name(headMention)
                    .targetLanguage(translateLanguage)
                    .entityType(type)
                    .build();

            NameTranslationResponse rntresponse;
            try {
                rntresponse = AccessController.doPrivileged((PrivilegedAction<NameTranslationResponse>) () ->
                        rosAPI.getHttpRosetteAPI().perform(NAME_TRANSLATION_SERVICE_PATH, rntrequest,
                                NameTranslationResponse.class)
                );
            } catch (HttpRosetteAPIException ex) {
                LOGGER.error(ex.getErrorResponse().getMessage());
                throw new ElasticsearchException(ex.getErrorResponse().getMessage(), ex);
            }
            toReturn.put("translation", rntresponse.getTranslation());
        }

        if (entity.getSentiment() != null) {
            toReturn.put("sentiment", entity.getSentiment().get(0).getLabel());
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

