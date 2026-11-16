package com.umbrella.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "umbrella")
public record UmbrellaProperties(List<String> allowedOrigins, String userAgent, Weather weather, Spots spots,
                                 Kakao kakao) {

    public record Weather(String baseUrl, String geocodeBaseUrl, Duration cacheTtl) {
    }

    public record Spots(List<String> endpoints, Duration timeout, Duration cacheTtl, Duration staleTtl) {
    }

    // restApiKey가 비어 있으면 카카오를 건너뛰고 OpenStreetMap(Overpass)으로 찾는다
    public record Kakao(String restApiKey, String baseUrl, Duration timeout) {

        public boolean enabled() {
            return restApiKey != null && !restApiKey.isBlank();
        }
    }
}
