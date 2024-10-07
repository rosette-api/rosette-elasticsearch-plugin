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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;

import java.io.Closeable;
import java.io.IOException;

/**
 * Class that holds code shared by all Rosette ingest processors
 */
public abstract class RosetteAbstractProcessor extends AbstractProcessor implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger("RosetteAbstractProcessor");

    protected String inputField;
    protected String targetField;
    protected RosetteApiWrapper rosAPI;
    protected final String processorType;

    RosetteAbstractProcessor(RosetteApiWrapper rosAPI, String tag, String description, String processorType,
                             String inputField, String targetField) {
        super(tag, description);
        this.inputField = inputField;
        this.targetField = targetField;
        this.rosAPI = rosAPI;
        this.processorType = processorType;
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Closing Rosette API client");
        if (rosAPI != null) {
            rosAPI.close();
        }
    }

    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {
        if (ingestDocument.hasField(targetField)) {
            throw new ElasticsearchException("Document already contains data in target field for this ingest "
                    + "processor: " + processorType);
        }
        if (!ingestDocument.hasField(inputField)) {
            //Do nothing
            return ingestDocument;
        }

        String inputText = ingestDocument.getFieldValue(inputField, String.class);
        if (Strings.isNullOrEmpty(inputText)) {
            //Do nothing
            return ingestDocument;
        }
        processDocument(inputText, ingestDocument);
        return ingestDocument;
    }

    @Override
    public String getType() {
        return this.processorType;
    }

    /**
     * Performs processor specific modifications to the document.
     * @param inputText value of the field in the document that was specified as the input field to process
     * @param ingestDocument document to be ingested
     * @throws Exception when something goes wrong
     */
    protected abstract void processDocument(String inputText, IngestDocument ingestDocument) throws Exception;
}
