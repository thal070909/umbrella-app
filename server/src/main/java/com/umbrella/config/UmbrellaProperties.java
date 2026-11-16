package com.umbrella.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "umbrella")
public record UmbrellaProperties(List<String> allowedOrigins, String userAgent, Weather weather, Spots spots) {

    public record Weather(String baseUrl, String geocodeBaseUrl, Duration cacheTtl) {
    }

    public record Spots(List<String> endpoints, Duration timeout, Duration cacheTtl, Duration staleTtl) {
    }
}
