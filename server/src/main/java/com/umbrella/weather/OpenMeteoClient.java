package com.umbrella.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.umbrella.config.UmbrellaProperties;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenMeteoClient {

    private static final String CURRENT = "temperature_2m,apparent_temperature,precipitation,weather_code,relative_humidity_2m";
    private static final String HOURLY = "precipitation_probability,precipitation";

    private final RestClient restClient;

    public OpenMeteoClient(RestClient.Builder externalRestClientBuilder, UmbrellaProperties properties) {
        this.restClient = externalRestClientBuilder.clone().baseUrl(properties.weather().baseUrl()).build();
    }

    // 오늘과 내일 시간별 값까지 받아서 "몇 시부터 비"를 서버가 계산한다 (timezone=auto라 시간 문자열은 현지 시각)
    public Forecast fetch(double lat, double lon) {
        return restClient.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("current", CURRENT)
                        .queryParam("hourly", HOURLY)
                        .queryParam("timezone", "auto")
                        .queryParam("forecast_days", 2)
                        .build())
                .retrieve()
                .body(Forecast.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Forecast(Current current, Hourly hourly) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            String time,
            @JsonProperty("temperature_2m") double temperature,
            @JsonProperty("apparent_temperature") double apparentTemperature,
            double precipitation,
            @JsonProperty("weather_code") int weatherCode,
            @JsonProperty("relative_humidity_2m") double relativeHumidity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Hourly(
            List<String> time,
            @JsonProperty("precipitation_probability") List<Integer> precipitationProbability,
            List<Double> precipitation) {
    }
}
