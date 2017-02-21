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

import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.ingest.WritePipelineResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.PluginInfo;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.hamcrest.ElasticsearchAssertions;
import org.hamcrest.Matchers;

import java.util.Collection;
import java.util.Collections;

public class RosetteTextAnalysisPluginIT extends ESIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                //.put(RosetteTextAnalysisPlugin.ROSETTE_API_KEY.getKey(), System.getProperty("rosette.api.key"))
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singleton(RosetteTextAnalysisPlugin.class);
    }

    public void testPluginIsLoaded() throws Exception {
        NodesInfoResponse response = client().admin().cluster().prepareNodesInfo().setPlugins(true).get();
        for (NodeInfo nodeInfo : response.getNodes()) {
            boolean pluginFound = false;
            for (PluginInfo pluginInfo : nodeInfo.getPlugins().getPluginInfos()) {
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

        //Add an ingest pipeline that includes the language processor
        WritePipelineResponse pipelineResponse = client().admin().cluster().preparePutPipeline("language", new BytesArray("{ \"processors\" : [ { \"" + LanguageProcessor.TYPE + "\" : { \"field\": \"text\" } } ] }")).get();
        assertTrue("Failed to add ingest pipeline", pipelineResponse.isAcknowledged());

        //Add a document that uses the ingest pipeline
        IndexResponse indexResponse = client().prepareIndex("test", "test").setPipeline("language").setSource(XContentFactory.jsonBuilder().startObject().field("text", inputText).endObject()).get();
        assertTrue("Failed to index document correctly", indexResponse.status().equals(RestStatus.CREATED));
        //Force index refresh
        refresh("test");

        //Find the document and check its source for the expected language identification
        SearchResponse response = client().prepareSearch("test").setQuery(QueryBuilders.matchAllQuery()).get();
        ElasticsearchAssertions.assertNoFailures(response);
        assertThat(response.getHits().getAt(0).getSource().get("ros_language"), Matchers.equalTo("eng"));
    }
}
