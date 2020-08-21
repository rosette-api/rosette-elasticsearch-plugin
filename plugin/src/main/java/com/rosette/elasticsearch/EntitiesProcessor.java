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
import com.basistech.rosette.apimodel.DocumentRequest;
import com.basistech.rosette.apimodel.EntitiesOptions;
import com.basistech.rosette.apimodel.NameTranslationRequest;
import com.basistech.rosette.apimodel.NameTranslationResponse;
import com.basistech.rosette.apimodel.SentimentOptions;
import com.basistech.rosette.dm.AnnotatedText;
import com.basistech.rosette.dm.Entity;
import com.basistech.rosette.dm.Mention;
import com.basistech.util.LanguageCode;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.logging.Loggers;
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

public class EntitiesProcessor extends RosetteAbstractProcessor {

    public static final String TYPE = "ros_entities";

    private static final Logger LOGGER = Loggers.getLogger(EntitiesProcessor.class, EntitiesProcessor.class.getName());

    private boolean includeOffsets;
    private boolean doTranslate;
    private LanguageCode translateLanguage;
    private boolean doSentiment;

    EntitiesProcessor(RosetteApiWrapper rosAPI, String tag, String description, String inputField, String targetField,
                      boolean includeOffsets, boolean doTranslate, LanguageCode translateLanguage,
                      boolean doSentiment) {
        super(rosAPI, tag, description, TYPE, inputField, targetField);
        this.includeOffsets = includeOffsets;
        this.doTranslate = doTranslate;
        this.translateLanguage = translateLanguage;
        this.doSentiment = doSentiment;
    }

    @Override
    public void processDocument(String inputText, IngestDocument ingestDocument) throws Exception {
        //Need to use the ADM for entities so we get offsets
        AnnotatedText adm;

        //If entity level sentiment is desired, use the entity information from the ASCENT call
        try {
            //SENTIMENT
            if (doSentiment) {
                DocumentRequest<SentimentOptions> sentrequest = DocumentRequest.<SentimentOptions>builder()
                        .content(inputText).build();
                adm = AccessController.doPrivileged((PrivilegedAction<AnnotatedText>) () ->
                        rosAPI.getHttpRosetteAPI().perform(AbstractRosetteAPI.SENTIMENT_SERVICE_PATH, sentrequest)
                );
            } else {
                //REX
                DocumentRequest<EntitiesOptions> entityrequest = DocumentRequest.<EntitiesOptions>builder()
                        .content(inputText).build();
                adm = AccessController.doPrivileged((PrivilegedAction<AnnotatedText>) () ->
                        rosAPI.getHttpRosetteAPI().perform(AbstractRosetteAPI.ENTITIES_SERVICE_PATH, entityrequest)
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
                && (type.equalsIgnoreCase("PERSON")
                || type.equalsIgnoreCase("LOCATION")
                || type.equalsIgnoreCase("ORGANIZATION"))) {
            NameTranslationRequest rntrequest = NameTranslationRequest.builder()
                    .name(headMention)
                    .targetLanguage(translateLanguage)
                    .entityType(type)
                    .build();

            NameTranslationResponse rntresponse;
            try {
                rntresponse = AccessController.doPrivileged((PrivilegedAction<NameTranslationResponse>) () ->
                        rosAPI.getHttpRosetteAPI().perform(AbstractRosetteAPI.NAME_TRANSLATION_SERVICE_PATH, rntrequest,
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
        TARGET_FIELD("target_field", "ros_entities"),
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
