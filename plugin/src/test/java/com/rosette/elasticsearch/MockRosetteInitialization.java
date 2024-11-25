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

import org.apache.http.HttpHeaders;
import org.mockserver.client.MockServerClient;
import org.mockserver.client.initialize.PluginExpectationInitializer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


//Mock-server is launched before unit tests run and serves up a mocked json response for each endpoint
public class MockRosetteInitialization implements PluginExpectationInitializer {

    @Override
    public void initializeExpectations(MockServerClient mockServerClient) {
        String baseURL = System.getProperty("mockserver.baseurl", "/rest/worker/v1/");

        //sentiment needs an ADM response when requested through entities
        HttpRequest sentimentADMReq = HttpRequest.request()
                .withMethod("POST")
                .withPath(baseURL + "sentiment")
                .withQueryStringParameter("output", "rosette");

        addEndpoint(mockServerClient, "mock_responses/sentiment_adm_response.json", sentimentADMReq);
        addEndpoint(baseURL, "categories", mockServerClient);
        addEndpoint(baseURL, "sentiment", mockServerClient);
        addEndpoint(baseURL, "language", mockServerClient);
        addEndpoint(baseURL, "entities", mockServerClient);
        addEndpoint(baseURL, "name-translation", mockServerClient);
    }

    private void addEndpoint(String baseURL, String endpointName, MockServerClient mockServerClient) {
        addEndpoint(mockServerClient, "mock_responses/" + endpointName + "_response.json",
                HttpRequest.request()
                .withMethod("POST")
                .withPath(baseURL + endpointName)
        );
    }

    private void addEndpoint(MockServerClient mockServerClient, String responseFile, HttpRequest req) {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(responseFile)) {
            String response = getStringFromResource(is);
            mockServerClient.when(req)
                    .respond(HttpResponse.response()
                            .withStatusCode(200)
                            .withHeaders(
                                    new Header(HttpHeaders.CONTENT_TYPE, "application/json")
                            )
                            .withBody(response));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private String getStringFromResource(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
