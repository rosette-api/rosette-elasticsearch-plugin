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

import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CategoriesProcessorTest extends ESTestCase {

    @Test
    public void testCategories() {
        try (CategoriesProcessor processor = new CategoriesProcessor(new RosetteApiWrapper(), randomUnicodeOfLength(10),
                "description", "text", "category")) {

            String inputText = "The people played lots of sports like soccer and hockey. The score was very high. "
                    + "Touchdown!";

            Map<String, Object> document = new HashMap<>();
            document.put("text", inputText);
            IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
            processor.execute(ingestDocument);

            MatcherAssert.assertThat(ingestDocument.getSourceAndMetadata().get("text"), Matchers.equalTo(inputText));
            MatcherAssert.assertThat(ingestDocument.getSourceAndMetadata().get("category"), Matchers.equalTo("SPORTS"));
        } catch (Exception e) {
            assertNull(e);
        }
    }
}
