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

import com.basistech.util.LanguageCode;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.hamcrest.Matchers;
import org.hamcrest.MatcherAssert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntitiesProcessorTest extends ESSingleNodeTestCase {

    private static final String INPUTTEXT = "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters "
            + "film, couldn’t be more pleased with the new all-female Ghostbusters cast, telling The Hollywood "
            + "Reporter, “The Aykroyd family is delighted by this inheritance of the Ghostbusters torch by these "
            + "most magnificent women in comedy.";

    public void testEntities() throws Exception {
        EntitiesProcessor processor = new EntitiesProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "entities", false, false,
                LanguageCode.ENGLISH, false);

        Map<String, Object> document = new HashMap<>();
        document.put("text", INPUTTEXT);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        processor.execute(ingestDocument);

        Map<String, Object> source = ingestDocument.getSourceAndMetadata();
        assertFalse("Entity processor failed to return entities", ((List)source.get("entities")).isEmpty());

        Map entity = (Map)((List)source.get("entities")).get(0);
        MatcherAssert.assertThat(entity.get("mention"), Matchers.equalTo("Dan Aykroyd"));
        //There shouldn't be any sentiment, translations, or offsets
        MatcherAssert.assertThat(entity.get("sentiment"), Matchers.nullValue());
        MatcherAssert.assertThat(entity.get("translation"), Matchers.nullValue());
        MatcherAssert.assertThat(entity.get("offsets"), Matchers.nullValue());
    }

    public void testOffsets() throws Exception {
        EntitiesProcessor processor = new EntitiesProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "entities", true, false,
                LanguageCode.ENGLISH, false);

        Map<String, Object> document = new HashMap<>();
        document.put("text", INPUTTEXT);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        processor.execute(ingestDocument);

        Map<String, Object> source = ingestDocument.getSourceAndMetadata();
        assertFalse("Entity processor failed to return entities", ((List)source.get("entities")).isEmpty());
        Map entity = (Map)((List)source.get("entities")).get(0);
        assertFalse("Entities are missing offsets", ((List)entity.get("offsets")).isEmpty());
        assertFalse("Entity offsets are empty", ((Map)((List)entity.get("offsets")).get(0)).isEmpty());

        MatcherAssert.assertThat(entity.get("sentiment"), Matchers.nullValue());
        MatcherAssert.assertThat(entity.get("translation"), Matchers.nullValue());
    }

    public void testSentiment() throws Exception {
        EntitiesProcessor processor = new EntitiesProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "entities", false, false,
                LanguageCode.ENGLISH, true);

        Map<String, Object> document = new HashMap<>();
        document.put("text", INPUTTEXT);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        processor.execute(ingestDocument);

        Map<String, Object> source = ingestDocument.getSourceAndMetadata();
        assertFalse("Entity processor failed to return entities", ((List)source.get("entities")).isEmpty());
        Map entity = (Map)((List)source.get("entities")).get(0);
        MatcherAssert.assertThat(entity.get("sentiment"), Matchers.anything());

        MatcherAssert.assertThat(entity.get("translation"), Matchers.nullValue());
        MatcherAssert.assertThat(entity.get("offsets"), Matchers.nullValue());
    }

    public void testTranslate() throws Exception {
        EntitiesProcessor processor = new EntitiesProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "entities", false, true,
                LanguageCode.KOREAN, false);

        Map<String, Object> document = new HashMap<>();
        document.put("text", INPUTTEXT);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        processor.execute(ingestDocument);

        Map<String, Object> source = ingestDocument.getSourceAndMetadata();
        assertFalse("Entity processor failed to return entities", ((List)source.get("entities")).isEmpty());
        Map entity = (Map)((List)source.get("entities")).get(0);
        MatcherAssert.assertThat(entity.get("translation"), Matchers.anything());

        MatcherAssert.assertThat(entity.get("sentiment"), Matchers.nullValue());
        MatcherAssert.assertThat(entity.get("offsets"), Matchers.nullValue());
    }
}
