package com.umbrella.weather;

import com.umbrella.weather.WeatherAdvice.Condition;
import com.umbrella.weather.WeatherAdvice.Outfit;
import com.umbrella.weather.WeatherAdvice.Umbrella;

public record WeatherResponse(
        String place,
        Condition condition,
        String message,
        int temperature,
        int feelsLike,
        double precipitationMm,
        int precipitationProbability,
        int humidity,
        Umbrella umbrella,
        Outfit outfit,
        boolean heatCarAlert,
        String observedAt) {
}
