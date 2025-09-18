package com.lincoco.springai.zhinao.autoconfigure.tool;

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
