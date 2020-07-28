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

import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.info.PluginsAndModules;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.PluginInfo;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.hamcrest.ElasticsearchAssertions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

//Tests all processors against an running embedded ES instance using the deployed Rosette API
public class RosetteTextAnalysisPluginIT extends ESIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(RosetteTextAnalysisPlugin.ROSETTE_API_KEY.getKey(), System.getProperty("rosette.api.key", ""))
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singleton(RosetteTextAnalysisPlugin.class);
    }

    public void testPluginIsLoaded() throws Exception {
        NodesInfoResponse response = client().admin().cluster().prepareNodesInfo().addMetric(NodesInfoRequest.Metric.PLUGINS.metricName()).get();
        for (NodeInfo nodeInfo : response.getNodes()) {
            boolean pluginFound = false;
            for (PluginInfo pluginInfo : nodeInfo.getInfo(PluginsAndModules.class).getPluginInfos()) {
                if (pluginInfo.getName().equals(RosetteTextAnalysisPlugin.class.getName())) {
                    pluginFound = true;
                    break;
                }
            }
            assertTrue(pluginFound);
        }
    }

    //Tests the language processor
    public void testLanguage() throws Exception {

        String inputText = "This is a very English document. It should be identified as English.";

        SearchResponse response = exercisePipeline(inputText, "language");

        //Check the source for the expected language
        MatcherAssert.assertThat(response.getHits().getAt(0).getSourceAsMap().get(LanguageProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("eng"));
    }

    public void testCategories() throws Exception {

        String inputText = "The people played lots of sports like soccer and hockey. The score was very high. Touchdown!";

        SearchResponse response = exercisePipeline(inputText, "categories");

        //Check the source for the expected category
        MatcherAssert.assertThat(response.getHits().getAt(0).getSourceAsMap().get(CategoriesProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("SPORTS"));
    }

    public void testSentiment() throws Exception {

        String inputText = "I love this sentence so much I want to marry it!";

        SearchResponse response = exercisePipeline(inputText, "sentiment");

        //Check the source for the expected sentiment
        MatcherAssert.assertThat(response.getHits().getAt(0).getSourceAsMap().get(SentimentProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("pos"));
    }

    public void testTranslateToEnglish() throws Exception {

        String inputText = "Владимир Путин";

        SearchResponse response = exercisePipeline(inputText, "translate_eng");

        //Check the source for the expected English translation
        MatcherAssert.assertThat(response.getHits().getAt(0).getSourceAsMap().get(NameTranslationProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("Vladimir Putin"));
    }

    public void testTranslateFromEnglish() throws Exception {
        String inputText = "Vladimir Putin";

        SearchResponse response = exercisePipeline(inputText, "translate_rus");

        //Check the source for the expected Russian translation
        MatcherAssert.assertThat(response.getHits().getAt(0).getSourceAsMap().get(NameTranslationProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("Владимир Путин"));
    }

    public void testEntities() throws Exception {

        String inputText = "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters film, couldn’t be more pleased with the new all-female Ghostbusters cast, telling The Hollywood Reporter, “The Aykroyd family is delighted by this inheritance of the Ghostbusters torch by these most magnificent women in comedy.”";

        SearchResponse response = exercisePipeline(inputText, "entities");

        //Check the source for the expected entity result
        assertFalse(((List)response.getHits().getAt(0).getSourceAsMap().get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).isEmpty());
        Map entity = (Map)((List)response.getHits().getAt(0).getSourceAsMap().get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).get(0);
        MatcherAssert.assertThat(entity.get("mention"), Matchers.equalTo("Original Ghostbuster"));
    }

    public void testEntitiesWithSentiment() throws Exception {

        String inputText = "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters film, couldn’t be more pleased with the new all-female Ghostbusters cast, telling The Hollywood Reporter, “The Aykroyd family is delighted by this inheritance of the Ghostbusters torch by these most magnificent women in comedy.”";

        SearchResponse response = exercisePipeline(inputText, "entities_sentiment");

        //Check the source for the expected entity level sentiment
        assertFalse(((List)response.getHits().getAt(0).getSourceAsMap().get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).isEmpty());
        Map entity = (Map)((List)response.getHits().getAt(0).getSourceAsMap().get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).get(0);
        MatcherAssert.assertThat(entity.get("mention"), Matchers.equalTo("Original Ghostbuster"));
        MatcherAssert.assertThat(entity.get("sentiment"), Matchers.equalTo("neu"));
    }

    //Test that all (or most) of the processors work together
    public void testAll() throws Exception {

        String inputText = "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters film, couldn’t be more pleased with the new all-female Ghostbusters cast, telling The Hollywood Reporter, “The Aykroyd family is delighted by this inheritance of the Ghostbusters torch by these most magnificent women in comedy.”";

        SearchResponse response = exercisePipeline(inputText, "all");

        //Check the source for the expected entity result
        Map<String, Object> source = response.getHits().getAt(0).getSourceAsMap();
        MatcherAssert.assertThat(source.get(LanguageProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("eng"));
        MatcherAssert.assertThat(source.get(CategoriesProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("ARTS_AND_ENTERTAINMENT"));
        MatcherAssert.assertThat(source.get(SentimentProcessor.Parameters.TARGET_FIELD.defaultValue), Matchers.equalTo("pos"));

        assertFalse(((List)source.get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).isEmpty());
        Map entity = (Map)((List)source.get(EntitiesProcessor.Parameters.TARGET_FIELD.defaultValue)).get(0);
        MatcherAssert.assertThat(entity.get("mention"), Matchers.equalTo("Original Ghostbuster"));
    }

    private SearchResponse exercisePipeline(String inputText, String pipelineName) throws IOException {

        //Add the ingest pipeline
        AcknowledgedResponse pipelineResponse = client().admin().cluster().preparePutPipeline(pipelineName, getProcessorConfig(pipelineName), XContentType.JSON).get();
        assertTrue("Failed to add ingest pipeline", pipelineResponse.isAcknowledged());

        //Add a document that uses the ingest pipeline
        IndexResponse indexResponse = client().prepareIndex("test", "test").setPipeline(pipelineName).setSource(XContentFactory.jsonBuilder().startObject().field("text", inputText).endObject()).get();
        assertTrue("Failed to index document correctly", indexResponse.status().equals(RestStatus.CREATED));
        //Force index refresh
        refresh("test");

        //Find the document
        SearchResponse response = client().prepareSearch("test").setQuery(QueryBuilders.matchAllQuery()).get();
        ElasticsearchAssertions.assertNoFailures(response);

        return response;
    }

    private BytesArray getProcessorConfig(String name) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("it_processors/" + name + ".json")) {
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8.name()))) {
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            return new BytesArray(sb.toString());
        }
    }
}
