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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RosetteAbstractProcessorTest extends ESTestCase {

    static class MockProcessor extends RosetteAbstractProcessor {
        MockProcessor(RosetteApiWrapper rosAPI, String tag, String description, String inputField, String targetField) {
            super(rosAPI, tag, description, "mock_processor", inputField, targetField);
        }

        @Override
        public void processDocument(String inputText, IngestDocument ingestDocument) {
            ingestDocument.setFieldValue(targetField, "Processed!");
        }
    }

    @Test(expected = ElasticsearchException.class)
    public void testOverwrite() throws Exception {
        try (MockProcessor processor = new MockProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "target")) {

            //Process document with a value already in the target field
            Map<String, Object> document = new HashMap<>();
            document.put("text", "input text");
            document.put("target", "don't overwrite me!");
            IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

            //We expect an exception to be thrown
            processor.execute(ingestDocument);
        }
    }
}
