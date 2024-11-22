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

import com.basistech.rosette.api.HttpRosetteAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

//Configures and holds on to the shared Rosette API client
public final class RosetteApiWrapper implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger("RosetteApiWrapper");
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
        System.out.println("url at start of wrapper constructor: " + altUrl);
        System.out.println("url in env: " + System.getenv("ROSETTE_API_URL"));
        if (Strings.isNullOrEmpty(apiKey)) {
            apiKey = System.getenv("ROSETTE_API_KEY");
        }
        if (Strings.isNullOrEmpty(altUrl)) {
            altUrl = System.getenv("ROSETTE_API_URL");
        }
        System.out.println("url: " + altUrl);

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


    @Override
    public void close() throws IOException {
        httpRosetteAPI.close();
    }

}
