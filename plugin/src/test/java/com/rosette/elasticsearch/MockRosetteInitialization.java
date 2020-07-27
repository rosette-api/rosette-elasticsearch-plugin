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

import org.apache.http.HttpHeaders;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.mockserver.client.initialize.PluginExpectationInitializer;

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
        //Specific case for when the EntityProcessor calls sentiment expecting an ADM
        //This has to be called before the other sentiment endpoint is added
        addSentimentADM(baseURL, mockServerClient);

        addEndpoint(baseURL, "categories", mockServerClient);
        addEndpoint(baseURL, "sentiment", mockServerClient);
        addEndpoint(baseURL, "language", mockServerClient);
        addEndpoint(baseURL, "entities", mockServerClient);
        addEndpoint(baseURL, "name-translation", mockServerClient);
    }

    private void addEndpoint(String baseURL, String endpointName, MockServerClient mockServerClient) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("mock_responses/" + endpointName + "_response.json")) {
            String response = getStringFromResource(is);
            mockServerClient.when(HttpRequest.request()
                    .withMethod("POST")
                    .withPath(baseURL + endpointName))
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

    private void addSentimentADM(String baseURL, MockServerClient mockServerClient) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("mock_responses/sentiment_adm_response.json")) {
            String response = getStringFromResource(is);
            mockServerClient.when(HttpRequest.request()
                    .withMethod("POST")
                    .withPath(baseURL + "sentiment").withQueryStringParameter(new Parameter("output", "rosette")))
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
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8.name()))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
