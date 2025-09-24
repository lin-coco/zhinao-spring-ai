/*
Copyright 2025 lin-coco

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package io.github.lincoco.zhinao.api;

import com.fasterxml.jackson.annotation.*;

import java.util.function.Function;


public class MockWeatherService implements Function<MockWeatherService.Request, MockWeatherService.Response> {


    @Override
    public Response apply(Request request) {
        double temperature = 0;
        if (request.location().contains("Paris")) {
            temperature = 15;
        } else if (request.location().contains("Tokyo")) {
            temperature = 10;
        } else if (request.location().contains("San Francisco")) {
            temperature = 30;
        }
        return new Response(temperature, 15, 20, 2, 53, 45, request.unit);
    }

    /**
     * Temperature units
     */
    public enum Unit {
        C("metric"),
        F("imperial");

        public final String unitName;
        Unit(String text) {
            this.unitName = text;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("Weather API request")
    public record Request(
            @JsonProperty(required = true, value = "location") @JsonPropertyDescription("The city and stat e.g. San Francisco, CA") String location,
            @JsonProperty("lat") @JsonPropertyDescription("The city latitude") double lat,
            @JsonProperty("lon") @JsonPropertyDescription("The city longitude") double lon,
            @JsonProperty(required = true, value = "unit") @JsonPropertyDescription("The temperature unit") Unit unit
    ) {

    }


    @JsonClassDescription("Weather API response")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            @JsonProperty("temperature") double temp,
            @JsonProperty("feels_like") double feelsLike,
            @JsonProperty("temp_min") double tempMin,
            @JsonProperty("temp_max") double tempMax,
            @JsonProperty("pressure") int pressure,
            @JsonProperty("humidity") int humidity,
            @JsonProperty("unit") Unit unit
    ) {
    }
}
