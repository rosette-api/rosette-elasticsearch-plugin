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

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.info.PluginsAndModules;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.PluginRuntimeInfo;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xcontent.XContentType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Tests all processors against a running embedded ES instance using the deployed Rosette API
public class RosetteTextAnalysisPluginIT extends ESIntegTestCase {

    @Override
    public void tearDown() throws Exception {
        logger.info("Shutting down ES");
        super.tearDown();
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal, Settings otherSettings) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal, otherSettings))
                .put(RosetteTextAnalysisPlugin.ROSETTE_API_KEY.getKey(), System.getProperty("rosette.api.key", ""))
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singleton(RosetteTextAnalysisPlugin.class);
    }

    @Test
    public void testPluginIsLoaded() {
        logger.info("Checking if plugin was loaded");
        NodesInfoResponse response = client().admin().cluster().prepareNodesInfo().get();
        boolean pluginFound = false;
        for (NodeInfo nodeInfo : response.getNodes()) {
            for (PluginRuntimeInfo pluginInfo : nodeInfo.getInfo(PluginsAndModules.class).getPluginInfos()) {
                String name = pluginInfo.descriptor().getName();
                if ("com.rosette.elasticsearch.RosetteTextAnalysisPlugin".equals(name)) {
                    pluginFound = true;
                    break;
                }
            }
            assertTrue(pluginFound);
        }
        logger.info("Plugin was loaded");
    }

    @Test
    public void testLanguage() throws Exception {
        String inputText = "This is a very English document. It should be identified as English.";

        logger.info("Testing language processor");
        GetResponse response = exerciseGetPipeline(inputText, "language");

        //Check the source for the expected language
        MatcherAssert.assertThat(response.getSourceAsMap()
                .get(LanguageProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("eng"));
        logger.info("Language processor test complete");
    }

    @Test
    public void testCategories() throws Exception {
        String inputText = "The people played lots of sports like soccer and hockey. The score was very high. "
                + "Touchdown!";

        logger.info("Testing categories processor");
        GetResponse response = exerciseGetPipeline(inputText, "categories");

        //Check the source for the expected category
        MatcherAssert.assertThat(response.getSourceAsMap()
                .get(CategoriesProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("SPORTS"));

        logger.info("Categories processor test complete");
    }

    @Test
    public void testSentiment() throws Exception {

        String inputText = "I love this sentence so much I want to marry it!";
        logger.info("Testing sentiment processor");
        GetResponse response = exerciseGetPipeline(inputText, "sentiment");

        //Check the source for the expected sentiment
        MatcherAssert.assertThat(response.getSourceAsMap()
                .get(SentimentProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("pos"));
        logger.info("Sentiment processor test complete");
    }

    @Test
    public void testTranslateToEnglish() throws Exception {

        String inputText = "Владимир Путин";
        logger.info("Testing name translation processor, translating from Russian to English");
        GetResponse response = exerciseGetPipeline(inputText, "translate_eng");

        //Check the source for the expected English translation
        MatcherAssert.assertThat(response.getSourceAsMap()
                .get(NameTranslationProcessor.Parameters.TARGET_FIELD.defaultValue),
                Matchers.equalTo("Vladimir Putin"));
        logger.info("Translating from Russian to English Name translation processor test complete");
    }

    @Test
    public void testTranslateFromEnglish() throws Exception {
        String inputText = "Vladimir Putin";

        logger.info("Testing name translation processor, translating from English to Russian");
        GetResponse response = exerciseGetPipeline(inputText, "translate_rus");

        //Check the source for the expected Russian translation
        MatcherAssert.assertThat(response.getSourceAsMap()
                .get(NameTranslationProcessor.Parameters.TARGET_FIELD.defaultValue),
                Matchers.equalTo("Владимир Путин"));
        logger.info("Name translation processor test complete");
    }

    @Test
    public void testEntities() throws Exception {

        String inputText = "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters film, couldn’t "
                + "be more pleased with the new all-female Ghostbusters cast, telling The Hollywood Reporter, “The "
                + "Aykroyd family is delighted by this inheritance of the Ghostbusters torch by these most magnificent "
                + "women in comedy.”";
        logger.info("Testing entities processor");
        GetResponse response = exerciseGetPipeline(inputText, "entities");

        //Check the source for the expected entity result
        assertFalse(((List<?>)response.getSourceAsMap()
                .get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).isEmpty());
        Map<?, ?> entity = (LinkedHashMap<?, ?>)((List<?>)response.getSourceAsMap()
                .get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).get(0);
        MatcherAssert.assertThat(entity.get("mention"), Matchers.equalTo("Original Ghostbuster Dan Aykroyd"));
        logger.info("Entities processor test complete");
    }

    @Test
    public void testEntitiesWithSentiment() throws Exception {

        String inputText = "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters film, couldn’t "
                + "be more pleased with the new all-female Ghostbusters cast, telling The Hollywood Reporter, “The "
                + "Aykroyd family is delighted by this inheritance of the Ghostbusters torch by these most magnificent "
                + "women in comedy.”";

        GetResponse response = exerciseGetPipeline(inputText, "entities_sentiment");

        //Check the source for the expected entity level sentiment
        assertFalse(((List<?>)response.getSourceAsMap()
                .get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).isEmpty());
        Map<?, ?> entity = (LinkedHashMap<?, ?>)((List<?>)response.getSourceAsMap()
                .get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).get(0);
        MatcherAssert.assertThat(entity.get("mention"), Matchers.equalTo("Original Ghostbuster Dan Aykroyd"));
        MatcherAssert.assertThat(entity.get("sentiment"), Matchers.equalTo("pos"));
    }

    //Test that all (or most) of the processors work together
    @Test
    public void testAll() throws Exception {

        String inputText = "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters film, couldn’t "
                + "be more pleased with the new all-female Ghostbusters cast, telling The Hollywood Reporter, “The "
                + "Aykroyd family is delighted by this inheritance of the Ghostbusters torch by these most magnificent "
                + "women in comedy.”";
        logger.info("Testing all processors together");
        GetResponse response = exerciseGetPipeline(inputText, "all");

        //Check the source for the expected entity result
        Map<String, Object> source = response.getSourceAsMap();
        MatcherAssert.assertThat(source.get(LanguageProcessor.Parameters.TARGET_FIELD.defaultValue),
                Matchers.equalTo("eng"));
        MatcherAssert.assertThat(source.get(CategoriesProcessor.Parameters.TARGET_FIELD.defaultValue),
                Matchers.equalTo("ARTS_AND_ENTERTAINMENT"));
        MatcherAssert.assertThat(source.get(SentimentProcessor.Parameters.TARGET_FIELD.defaultValue),
                Matchers.equalTo("pos"));

        assertFalse(((List<?>)source.get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).isEmpty());
        Map<?, ?> entity = (LinkedHashMap<?, ?>)((List<?>)source.get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).get(0);
        MatcherAssert.assertThat(entity.get("mention"), Matchers.equalTo("Original Ghostbuster Dan Aykroyd"));
        logger.info("All processors test complete");
    }
    private GetResponse exerciseGetPipeline(String inputText, String pipelineName) throws IOException {
        //Add the ingest pipeline
        AcknowledgedResponse pipelineResponse = client().admin().cluster()
                .preparePutPipeline(pipelineName, getProcessorConfig(pipelineName), XContentType.JSON).get();
        assertTrue("Failed to add ingest pipeline", pipelineResponse.isAcknowledged());
        //Add a document that uses the ingest pipeline
        DocWriteResponse docWriteResponse = client().prepareIndex("test").setId("myid").setPipeline(pipelineName)
                .setSource("text", inputText).get();
        assertEquals("Failed to index document correctly", RestStatus.CREATED, docWriteResponse.status());
        GetResponse getResponse = client().prepareGet("test", "myid").get();
        assertTrue("Failed to find indexed document", getResponse.isExists());
        return getResponse;
    }
    private BytesArray getProcessorConfig(String name) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("it_processors/" + name + ".json")) {
            StringBuilder sb = new StringBuilder();
            assert is != null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            return new BytesArray(sb.toString());
        }
    }
}
