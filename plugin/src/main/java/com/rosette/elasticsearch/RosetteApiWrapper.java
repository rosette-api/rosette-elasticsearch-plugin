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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;

import com.basistech.rosette.api.HttpRosetteAPI;
import org.elasticsearch.common.logging.Loggers;

//Configures and holds on to the shared Rosette API client
public final class RosetteApiWrapper {

    private static final Logger LOGGER = Loggers.getLogger(RosetteApiWrapper.class, RosetteApiWrapper.class.getName());

    private static final String APP_HEADER;
    static {
        Properties props = new Properties();
        String appHeader = "";
        try (InputStream ins = RosetteApiWrapper.class.getClassLoader()
                .getResourceAsStream("plugin-descriptor.properties")) {
            props.load(ins);
            String pluginName = props.getProperty("classname")
                    .substring(props.getProperty("classname").lastIndexOf('.') + 1);
            String pluginVersion = props.getProperty("version");
            String elasticVersion = props.getProperty("elasticsearch.version");
            appHeader = String.format("%s-%s/ElasticSearch-%s", pluginName, pluginVersion, elasticVersion);
        } catch (IOException e) {
            // unreachable or the plugin is broken
        } finally {
            APP_HEADER = appHeader;
        }
    }

    // TODO: revisit this when we use embedded client
    private HttpRosetteAPI httpRosetteAPI;

    RosetteApiWrapper() {
        this(null, null);
    }

    RosetteApiWrapper(String apiKey, String altUrl) {
        if (Strings.isNullOrEmpty(apiKey)) {
            apiKey = System.getenv("ROSETTE_API_KEY");
        }

        if (Strings.isNullOrEmpty(altUrl)) {
            altUrl = System.getenv("ROSETTE_API_URL");
        }

        if ((HttpRosetteAPI.DEFAULT_URL_BASE.equalsIgnoreCase(altUrl) || Strings.isNullOrEmpty(altUrl))
                && Strings.isNullOrEmpty(apiKey)) {
            throw new ElasticsearchException("Rosette plugin requires setting an API Key either via the '"
                    + RosetteTextAnalysisPlugin.ROSETTE_API_KEY.getKey()
                    + "' setting, or the 'ROSETTE_API_KEY' environment variable.");
        }

        HttpRosetteAPI.Builder clientBuilder = new HttpRosetteAPI.Builder();
        clientBuilder.key(apiKey).additionalHeader("X-RosetteAPI-App", APP_HEADER);
        if (!Strings.isNullOrEmpty(altUrl)) {
            LOGGER.info("Using alternative URL for Rosette API at : {} ", altUrl);
            clientBuilder.url(altUrl);
        }
        httpRosetteAPI = clientBuilder.build();
    }

    public HttpRosetteAPI getHttpRosetteAPI() {
        return httpRosetteAPI;
    }
}
