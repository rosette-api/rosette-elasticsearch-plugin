/*
* Copyright 2020 Basis Technology Corp.
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
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RosetteAbstractProcessorTest extends ESSingleNodeTestCase {

    class MockProcessor extends RosetteAbstractProcessor {
        MockProcessor(RosetteApiWrapper rosAPI, String tag, String description, String inputField, String targetField) {
            super(rosAPI, tag, description, "mock_processor", inputField, targetField);
        }

        @Override
        public void processDocument(String inputText, IngestDocument ingestDocument) throws Exception {
            ingestDocument.setFieldValue(targetField, "Processed!");
        }
    }

    @Test
    public void testEmptyField() throws Exception {
        MockProcessor processor = new MockProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "target");

        //Process document with an empty "text" field
        Map<String, Object> document = new HashMap<>();
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        processor.execute(ingestDocument);

        //Nothing should get placed in the target field
        MatcherAssert.assertThat("Processor should not process empty ingest field",
                ingestDocument.getSourceAndMetadata().get("target"), Matchers.nullValue());
    }

    @Test(expected = ElasticsearchException.class)
    public void testOverwrite() throws Exception {
        MockProcessor processor = new MockProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "target");

        //Process document with a value already in the target field
        Map<String, Object> document = new HashMap<>();
        document.put("text", "input text");
        document.put("target", "don't overwrite me!");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);

        //We expect an exception to be thrown
        processor.execute(ingestDocument);
    }
}
