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
import com.basistech.rosette.apimodel.CategoriesOptions;
import com.basistech.rosette.apimodel.CategoriesResponse;
import com.basistech.rosette.apimodel.DocumentRequest;
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

public class CategoriesProcessor extends RosetteAbstractProcessor {
    public static final String TYPE = "ros_categories";
    private static final Logger LOGGER = LogManager.getLogger(CategoriesProcessor.class);
    private static final String CATEGORIES_SERVICE_PATH = "/categories";

    CategoriesProcessor(RosetteApiWrapper rosAPI, String tag, String description, String inputField,
                        String targetField) {
        super(rosAPI, tag, description, TYPE, inputField, targetField);
    }

    @Override
    public void processDocument(String inputText, IngestDocument ingestDocument) throws ElasticsearchException {
        // call /categories endpoint and set the top result in the field
        DocumentRequest<CategoriesOptions> request = DocumentRequest.<CategoriesOptions>builder()
                .content(inputText).build();
        CategoriesResponse response;
        try {
            // RosApi client binding's Jackson needs elevated privilege
            response = AccessController.doPrivileged((PrivilegedAction<CategoriesResponse>) () ->
                    rosAPI.getHttpRosetteAPI().perform(CATEGORIES_SERVICE_PATH, request, CategoriesResponse.class)
            );
        } catch (HttpRosetteAPIException ex) {
            LOGGER.error(ex.getErrorResponse().getMessage());
            throw new ElasticsearchException(ex.getErrorResponse().getMessage(), ex);
        }

        if (response.getCategories() != null
                && !response.getCategories().isEmpty()
                && response.getCategories().get(0) != null
                && !Strings.isNullOrEmpty(response.getCategories().get(0).getLabel())) {
            ingestDocument.setFieldValue(targetField, response.getCategories().get(0).getLabel());
        } else {
            throw new ElasticsearchException(TYPE + " ingest processor failed to categorize document.");
        }
    }

    public static final class Factory implements Processor.Factory {
        private RosetteApiWrapper rosAPI;

        Factory(RosetteApiWrapper rosAPI) {
            this.rosAPI = rosAPI;
        }

        @Override
        public Processor create(Map<String, Processor.Factory> registry, String processorTag,
                                String processorDescription, Map<String, Object> config) {
            String inputField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config, "field");
            String targetField = ConfigurationUtils.readStringProperty(TYPE, processorTag, config,
                    Parameters.TARGET_FIELD.name, Parameters.TARGET_FIELD.defaultValue);
            return new CategoriesProcessor(rosAPI, processorTag, processorDescription, inputField, targetField);
        }
    }

    enum Parameters {
        TARGET_FIELD("target_field", "ros_category");

        final String name;
        final String defaultValue;

        Parameters(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }
    }
}
