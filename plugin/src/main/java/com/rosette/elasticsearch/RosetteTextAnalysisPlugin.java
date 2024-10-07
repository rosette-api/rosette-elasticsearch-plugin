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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.ingest.Processor;
import org.elasticsearch.plugins.IngestPlugin;
import org.elasticsearch.plugins.MapperPlugin;
import org.elasticsearch.plugins.Plugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RosetteTextAnalysisPlugin extends Plugin implements AutoCloseable, MapperPlugin, IngestPlugin {
    public static final Setting<String> ROSETTE_API_KEY =
            Setting.simpleString("ingest.rosette.api_key", Setting.Property.NodeScope, Setting.Property.Filtered);
    public static final Setting<String> ROSETTE_API_URL =
            Setting.simpleString("ingest.rosette.api_url", Setting.Property.NodeScope, Setting.Property.Filtered);
    private static final Logger LOGGER = LogManager.getLogger("RosetteAnalysisPlugin");
    private RosetteApiWrapper rosAPI;

    @Override
    public List<Setting<?>> getSettings() {
        return Arrays.asList(ROSETTE_API_KEY, ROSETTE_API_URL);
    }
    @Override
    public void close() throws IOException {
        LOGGER.info("Closing Rosette API client");
        if (rosAPI != null) {
            rosAPI.close();
        }
    }
    @Override
    public Map<String, Processor.Factory> getProcessors(Processor.Parameters parameters) {
        String key = ROSETTE_API_KEY.get(parameters.env.settings());
        String altURL = ROSETTE_API_URL.get(parameters.env.settings());
        //As this method is called at Node startup, this should ensure only one instance of the api client
        if (rosAPI == null) {
            LOGGER.info("Creating Rosette API client");
            rosAPI = new RosetteApiWrapper(key, altURL);
        }
        Map<String, Processor.Factory> processors = new HashMap<>();
        processors.put(LanguageProcessor.TYPE, new LanguageProcessor.Factory(rosAPI));
        processors.put(CategoriesProcessor.TYPE, new CategoriesProcessor.Factory(rosAPI));
        processors.put(SentimentProcessor.TYPE, new SentimentProcessor.Factory(rosAPI));
        processors.put(NameTranslationProcessor.TYPE, new NameTranslationProcessor.Factory(rosAPI));
        processors.put(EntitiesProcessor.TYPE, new EntitiesProcessor.Factory(rosAPI));
        return processors;
    }
}
