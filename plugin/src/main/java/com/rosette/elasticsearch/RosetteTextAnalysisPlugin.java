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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.plugins.IngestPlugin;
import org.elasticsearch.plugins.MapperPlugin;
import org.elasticsearch.plugins.Plugin;

public class RosetteTextAnalysisPlugin extends Plugin implements MapperPlugin, IngestPlugin {

    public static final Setting<String> ROSETTE_API_KEY =
            Setting.simpleString("ingest.rosette.api_key", Setting.Property.NodeScope, Setting.Property.Filtered);
    public static final Setting<String> ROSETTE_API_URL =
            Setting.simpleString("ingest.rosette.api_url", Setting.Property.NodeScope, Setting.Property.Filtered);

    @Override
    public List<Setting<?>> getSettings() {
        return Arrays.asList(ROSETTE_API_KEY, ROSETTE_API_URL);
    }

    @Override
    public Map<String, Processor.Factory> getProcessors(Processor.Parameters parameters) {
        String key = ROSETTE_API_KEY.get(parameters.env.settings());
        String altURL = ROSETTE_API_URL.get(parameters.env.settings());
        //As this method is called at Node startup, this should ensure only one instance of the api client
        RosetteApiWrapper rosAPI = new RosetteApiWrapper(key, altURL);

        return Collections.singletonMap(LanguageProcessor.TYPE, new LanguageProcessor.Factory(rosAPI));
    }
}
