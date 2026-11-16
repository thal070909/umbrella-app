package com.umbrella.weather;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.umbrella.common.GeoGrid;
import com.umbrella.config.UmbrellaProperties;
import com.umbrella.weather.OpenMeteoClient.Current;
import com.umbrella.weather.OpenMeteoClient.Forecast;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    // 약 1km. 같은 동네 사람은 날씨가 같다
    static final double GRID_DEGREES = 0.01;

    private final OpenMeteoClient openMeteoClient;
    private final ReverseGeocodeClient reverseGeocodeClient;
    private final Cache<String, WeatherResponse> cache;

    public WeatherService(OpenMeteoClient openMeteoClient, ReverseGeocodeClient reverseGeocodeClient,
                          UmbrellaProperties properties) {
        this.openMeteoClient = openMeteoClient;
        this.reverseGeocodeClient = reverseGeocodeClient;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(properties.weather().cacheTtl())
                .maximumSize(10_000)
                .build();
    }

    public WeatherResponse weather(double lat, double lon) {
        GeoGrid grid = GeoGrid.of(lat, lon, GRID_DEGREES);
        return cache.get(grid.key(), key -> load(grid));
    }

    private WeatherResponse load(GeoGrid grid) {
        Forecast forecast = openMeteoClient.fetch(grid.lat(), grid.lon());
        if (forecast == null || forecast.current() == null) {
            throw new IllegalStateException("날씨 응답이 비어 있습니다");
        }
        String place = reverseGeocodeClient.placeName(grid.lat(), grid.lon());
        return toResponse(place, forecast);
    }

    static WeatherResponse toResponse(String place, Forecast forecast) {
        Current current = forecast.current();
        WeatherAdvice.Condition condition = WeatherAdvice.condition(current.weatherCode());
        int temperature = (int) Math.round(current.temperature());
        int feelsLike = (int) Math.round(current.apparentTemperature());
        return new WeatherResponse(
                place,
                condition,
                WeatherAdvice.message(condition, temperature),
                temperature,
                feelsLike,
                current.precipitation(),
                WeatherAdvice.probabilityNow(current.time(), forecast.hourly()),
                (int) Math.round(current.relativeHumidity()),
                WeatherAdvice.umbrella(current, forecast.hourly()),
                WeatherAdvice.outfit(feelsLike),
                WeatherAdvice.heatCarAlert(temperature),
                current.time());
    }
}
