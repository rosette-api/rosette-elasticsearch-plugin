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

import org.elasticsearch.common.Strings;

import com.basistech.rosette.api.HttpRosetteAPI;

public final class RosetteApiWrapper {

    private static RosetteApiWrapper instance;

    // TODO: revisit this when we use embedded client
    private HttpRosetteAPI httpRosetteAPI;

    private RosetteApiWrapper() {
        HttpRosetteAPI.Builder clientBuilder = new HttpRosetteAPI.Builder();
        String apiKey = System.getenv("ROSETTE_API_KEY");
        String altUrl = System.getenv("ROSETTE_API_URL");
        clientBuilder.key(apiKey);
        if (Strings.isNullOrEmpty(altUrl)) {
            clientBuilder.url(altUrl);
        }
        httpRosetteAPI = clientBuilder.build();
    }

    public static RosetteApiWrapper getInstance() {
        if (instance == null) {
            instance = new RosetteApiWrapper();
        }
        return instance;
    }

    public HttpRosetteAPI getHttpRosetteAPI() {
        return httpRosetteAPI;
    }
}
