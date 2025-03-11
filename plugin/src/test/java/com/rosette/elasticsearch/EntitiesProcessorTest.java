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
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntitiesProcessorTest extends ESTestCase {

    private static final String INPUTTEXT = "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters "
            + "film, couldn’t be more pleased with the new all-female Ghostbusters cast, telling The Hollywood "
            + "Reporter, “The Aykroyd family is delighted by this inheritance of the Ghostbusters torch by these "
            + "most magnificent women in comedy.";

    @Test
    public void testEntities() {
        try (EntitiesProcessor processor = new EntitiesProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "entities", false, false,
                LanguageCode.ENGLISH, false)) {

            Map<String, Object> document = new HashMap<>();
            document.put("text", INPUTTEXT);
            IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
            processor.execute(ingestDocument);

            Map<String, Object> source = ingestDocument.getSourceAndMetadata();
            assertFalse("Entity processor failed to return entities", ((List<?>) source.get("entities")).isEmpty());

            Map<?, ?> entity = (HashMap<?, ?>) ((List<?>) source.get("entities")).get(0);
            assertTrue(((String) entity.get("mention")).contains("Dan Aykroyd"));
            //There shouldn't be any sentiment, translations, or offsets
            MatcherAssert.assertThat(entity.get("sentiment"), Matchers.nullValue());
            MatcherAssert.assertThat(entity.get("translation"), Matchers.nullValue());
            MatcherAssert.assertThat(entity.get("offsets"), Matchers.nullValue());
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void testOffsets() {
        try (EntitiesProcessor processor = new EntitiesProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "entities", true, false,
                LanguageCode.ENGLISH, false)) {

            Map<String, Object> document = new HashMap<>();
            document.put("text", INPUTTEXT);
            IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
            processor.execute(ingestDocument);

            Map<String, Object> source = ingestDocument.getSourceAndMetadata();
            assertFalse("Entity processor failed to return entities", ((List<?>) source.get("entities")).isEmpty());
            Map<?, ?> entity = (HashMap<?, ?>) ((List<?>) source.get("entities")).get(0);
            assertFalse("Entities are missing offsets", ((List<?>) entity.get("offsets")).isEmpty());
            assertFalse("Entity offsets are empty", ((Map<?, ?>) ((List<?>) entity.get("offsets")).get(0)).isEmpty());

            MatcherAssert.assertThat(entity.get("sentiment"), Matchers.nullValue());
            MatcherAssert.assertThat(entity.get("translation"), Matchers.nullValue());
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void testSentiment() {
        try (EntitiesProcessor processor = new EntitiesProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "entities", false, false,
                LanguageCode.ENGLISH, true)) {

            Map<String, Object> document = new HashMap<>();
            document.put("text", INPUTTEXT);
            IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
            processor.execute(ingestDocument);

            Map<String, Object> source = ingestDocument.getSourceAndMetadata();
            assertFalse("Entity processor failed to return entities", ((List<?>) source.get("entities")).isEmpty());
            Map<?, ?> entity = (HashMap<?, ?>) ((List<?>) source.get("entities")).get(0);
            MatcherAssert.assertThat(entity.get("sentiment"), Matchers.anything());

            MatcherAssert.assertThat(entity.get("translation"), Matchers.nullValue());
            MatcherAssert.assertThat(entity.get("offsets"), Matchers.nullValue());
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test
    public void testTranslate() {
        try (EntitiesProcessor processor = new EntitiesProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "entities", false, true,
                LanguageCode.KOREAN, false)) {

            Map<String, Object> document = new HashMap<>();
            document.put("text", INPUTTEXT);
            IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
            processor.execute(ingestDocument);

            Map<String, Object> source = ingestDocument.getSourceAndMetadata();
            assertFalse("Entity processor failed to return entities", ((List<?>) source.get("entities")).isEmpty());
            Map<?, ?> entity = (HashMap<?, ?>) ((List<?>) source.get("entities")).get(0);
            MatcherAssert.assertThat(entity.get("translation"), Matchers.anything());

            MatcherAssert.assertThat(entity.get("sentiment"), Matchers.nullValue());
            MatcherAssert.assertThat(entity.get("offsets"), Matchers.nullValue());
        } catch (Exception e) {
            assertNull(e);
        }
    }
}
