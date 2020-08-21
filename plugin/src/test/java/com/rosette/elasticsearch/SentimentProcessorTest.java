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

import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import java.util.HashMap;
import java.util.Map;

public class SentimentProcessorTest extends ESSingleNodeTestCase {

    public void testSentiment() throws Exception {
        SentimentProcessor processor = new SentimentProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "sentiment");

        String inputText = "I love this sentence so much I want to marry it!";

        Map<String, Object> document = new HashMap<>();
        document.put("text", inputText);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        processor.execute(ingestDocument);

        MatcherAssert.assertThat(ingestDocument.getSourceAndMetadata().get("text"), Matchers.equalTo(inputText));
        MatcherAssert.assertThat(ingestDocument.getSourceAndMetadata().get("sentiment"), Matchers.equalTo("pos"));
    }
}
