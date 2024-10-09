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

import com.basistech.util.ISO15924;
import com.basistech.util.LanguageCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

//Configures and holds on to the shared Rosette API client
public final class RosetteApiWrapper implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger("RosetteApiWrapper");
    private static final int MAX_CONNECTIONS = 10;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 10;

    private static final String DEFAULT_URL_BASE = "https://api.rosette.com/rest/v1";
    private static final String APP_HEADER;
    private final String targetURL;
    private CloseableHttpClient httpClient;
    private boolean closed = true;
    private final ObjectMapper objectMapper;

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

    RosetteApiWrapper() {
        this(null, null);
    }

    RosetteApiWrapper(String apiKey, String altUrl) {
        LOGGER.info("Creating Rosette API client");
        objectMapper = new ObjectMapper();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(MAX_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);

        HttpClientBuilder builder = HttpClients.custom();
        Collection<Header> defaultHeaders = new ArrayList<>();
        defaultHeaders.add(new BasicHeader("Accept", "application/json"));
        defaultHeaders.add(new BasicHeader("Content-Type", "application/json"));
        defaultHeaders.add(new BasicHeader("X-RosetteAPI-App", APP_HEADER));
        builder.setDefaultHeaders(defaultHeaders);
        builder.setConnectionManager(connectionManager);

        if (Strings.isNullOrEmpty(apiKey)) {
            apiKey = System.getenv("ROSETTE_API_KEY");
        }
        if (Strings.isNullOrEmpty(altUrl)) {
            altUrl = System.getenv("ROSETTE_API_URL");
        }
        if (!Strings.isNullOrEmpty(altUrl)) {
            LOGGER.info("Using URL for Rosette API: {} ", altUrl);
            if (altUrl.charAt(altUrl.length() - 1) == '/') {
                altUrl = altUrl.substring(0, altUrl.length() - 1);
            }
            targetURL = altUrl;
        } else {
            LOGGER.info("Using default URL for Rosette API at : {} ", DEFAULT_URL_BASE);
            targetURL = DEFAULT_URL_BASE;
        }
        if ((DEFAULT_URL_BASE.equalsIgnoreCase(altUrl) || Strings.isNullOrEmpty(altUrl))
                && Strings.isNullOrEmpty(apiKey)) {
            throw new ElasticsearchException("Rosette plugin requires setting an API Key either via the '"
                    + RosetteTextAnalysisPlugin.ROSETTE_API_KEY.getKey()
                    + "' setting, or the 'ROSETTE_API_KEY' environment variable.");
        }
        if (apiKey != null) {
            defaultHeaders.add(new BasicHeader("X-RosetteAPI-Key", apiKey));
        }
        httpClient = builder.build();
        closed = false;
    }


    @Override
    public void close() throws IOException {
        if (!closed && httpClient != null) {
            LOGGER.info("Closing Rosette API client");
            httpClient.close();
            httpClient = null;
            closed = true;
        }
    }
    private JsonNode makePostRequest(HttpPost request) throws HttpClientException, HttpServerException {
        LOGGER.info("Calling Rosette API with request: {}", request.getRequestUri());
        CloseableHttpResponse response = null;
        try {
            response =
                    AccessController.doPrivileged(
                            (PrivilegedAction<CloseableHttpResponse>) () -> {
                                try {
                                    LOGGER.info("Executing request: {}", request.getRequestUri());
                                    return httpClient.execute(request);
                                } catch (IOException e) {
                                    throw new HttpClientRuntimeException(e.getMessage());
                                }
                            });
            LOGGER.info("Response code: {}", response.getCode());
            checkResponseCode(response);
            // Parse the JSON string into a JsonNode
            return objectMapper.readTree(EntityUtils.toString(response.getEntity()));
        } catch (HttpClientRuntimeException rte) {
            LOGGER.error("Error connecting to Rosette API", rte);
            throw new ElasticsearchException("Error connecting to Rosette API");
        } catch (IOException | ParseException e) {
            LOGGER.error("Error while performing request to name-translation Rosette API ", e);
            throw new ElasticsearchException("Error while performing request to name-translation Rosette API", e);
        } finally {
            request.clear();
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                LOGGER.error("Error while closing response and client", e);
            }
        }
    }
    public JsonNode performNameTranslationRequest(String path, String name, LanguageCode targetLanguage,
                                                  String entityType, ISO15924 targetScript,
                                                  LanguageCode sourceLanguageOfUse, LanguageCode sourceLanguageOfOrigin,
                                                  ISO15924 sourceScript)
            throws HttpClientException, HttpServerException {
        if (!path.startsWith("/")) {
            path = "/".concat(path);
        }
        LOGGER.info("Performing name translation request for name: {} to {}",
                name, targetLanguage != null ? targetLanguage.languageName() : "unknown");
        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("name", name);
        if (entityType != null) {
            bodyMap.put("entityType", entityType);
        }
        if (sourceScript != null && sourceScript != ISO15924.Zyyy) {
            bodyMap.put("sourceScript", sourceScript.code4());
        }
        if (sourceLanguageOfUse != null) {
            bodyMap.put("sourceLanguageOfUse", sourceLanguageOfUse.ISO639_3());
        }
        if (sourceLanguageOfOrigin != null) {
            bodyMap.put("sourceLanguageOfOrigin", sourceLanguageOfOrigin.ISO639_3());
        }
        if (targetLanguage != null) {
            bodyMap.put("targetLanguage", targetLanguage.ISO639_3());
        }
        if (targetScript != null && sourceScript != ISO15924.Zyyy) {
            bodyMap.put("targetScript", targetScript.code4());
        }
        try (StringEntity payload =
                     new StringEntity(objectMapper.writeValueAsString(bodyMap), ContentType.APPLICATION_JSON)) {
            HttpPost request = new HttpPost(targetURL + path);
            request.setEntity(payload);
            return makePostRequest(request);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error generating content for name translation request", e);
        } catch (IOException e) {
            LOGGER.error("Error while closing payload", e);
        }
        return null;
    }

    public JsonNode performDocumentRequest(String path, String content, String language)
            throws HttpClientException, HttpServerException {
        if (!path.startsWith("/")) {
            path = "/".concat(path);
        }
        LOGGER.info("Performing document request for path: {}", path);

        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("content", content);
        if (language != null) {
            bodyMap.put("language", language);
        }
        try (StringEntity payload =
                     new StringEntity(objectMapper.writeValueAsString(bodyMap), ContentType.APPLICATION_JSON)) {
            HttpPost docRequest = new HttpPost(targetURL + path);
            docRequest.setEntity(payload);
            return makePostRequest(docRequest);
        } catch (IOException e) {
            LOGGER.error("Error while closing payload", e);
        }
        return null;
    }

    private void checkResponseCode(HttpResponse response) throws HttpClientException, HttpServerException {
        if (response.getCode() == HttpStatus.SC_OK) {
            return;
        }
        int code = response.getCode();
        if (code >= 400 && code < 500) {
            throw new HttpClientException(response.getReasonPhrase());
        }
        if (code >= 500 && code < 600) {
            throw new HttpServerException(response.getReasonPhrase());
        }
    }

}
