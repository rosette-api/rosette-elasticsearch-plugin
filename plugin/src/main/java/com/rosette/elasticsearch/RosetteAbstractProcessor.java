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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.common.Strings;
import org.elasticsearch.ingest.AbstractProcessor;
import org.elasticsearch.ingest.IngestDocument;

/**
 * Class that holds code shared by all Rosette ingest processors
 */
public abstract class RosetteAbstractProcessor extends AbstractProcessor {

    protected String inputField;
    protected String targetField;
    protected RosetteApiWrapper rosAPI;
    protected final String type;

    RosetteAbstractProcessor(RosetteApiWrapper rosAPI, String tag, String processorType, String inputField, String targetField) {
        super(tag);
        this.inputField = inputField;
        this.targetField = targetField;
        this.rosAPI = rosAPI;
        this.type = processorType;
    }

    @Override
    public IngestDocument execute(IngestDocument ingestDocument) throws Exception {
        if (ingestDocument.hasField(targetField)) {
            throw new ElasticsearchException("Document already contains data in target field for this ingest processor: " + type);
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

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }

        processDocument(inputText, ingestDocument);
        return ingestDocument;
    }

    @Override
    public String getType() {
        return this.type;
    }

    /**
     * Performs processor specific modifications to the document.
     * @param inputText value of the field in the document that was specified as the input field to process
     * @param ingestDocument document to be ingested
     * @throws Exception
     */
    protected abstract void processDocument(String inputText, IngestDocument ingestDocument) throws Exception;
}
