package com.lifetracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class WeatherService {
    
    @Value("${weather.api.key:demo}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String OPENWEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    /**
     * Get weather for a specific location
     */
    public Map<String, Object> getWeatherByLocation(String location) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // For demo purposes, return simulated data if API key is "demo"
            if ("demo".equals(apiKey)) {
                return getSimulatedWeather(location);
            }
            
            String url = String.format("%s?q=%s&appid=%s&units=metric", 
                    OPENWEATHER_BASE_URL, location, apiKey);
            
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);
            
            result.put("location", location);
            result.put("temperature", node.path("main").path("temp").asDouble());
            result.put("feelsLike", node.path("main").path("feels_like").asDouble());
            result.put("humidity", node.path("main").path("humidity").asInt());
            // OpenWeather wind speed is in m/s when units=metric. Convert to km/h for UI consistency.
            result.put("windSpeed", node.path("wind").path("speed").asDouble() * 3.6);
            result.put("condition", node.path("weather").get(0).path("main").asText());
            result.put("description", node.path("weather").get(0).path("description").asText());
            result.put("success", true);
            
            return result;
        } catch (RestClientException e) {
            // Fallback to simulated weather
            return getSimulatedWeather(location);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Unable to fetch weather data");
            return error;
        }
    }
    
    /**
     * Get simulated weather data for demo/testing
     */
    private Map<String, Object> getSimulatedWeather(String location) {
        Map<String, Object> result = new HashMap<>();

        String normalizedLocation = location == null ? "unknown" : location.trim().toLowerCase();

        // Start from location-seeded pseudo-random values so different locations produce different weather.
        Random random = new Random(normalizedLocation.hashCode());
        String condition = "Clear";
        String description = "clear sky";
        double temp = 18.0 + (random.nextDouble() * 16.0); // 18.0 - 34.0
        int humidity = 40 + random.nextInt(51); // 40 - 90
        double windSpeed = 3.0 + (random.nextDouble() * 19.0); // 3.0 - 22.0 km/h

        if (normalizedLocation.contains("rain") || normalizedLocation.contains("london")) {
            condition = "Rainy";
            description = "heavy rain";
            temp = 15.0;
            humidity = 85;
            windSpeed = 12.5;
        } else if (normalizedLocation.contains("snow") || normalizedLocation.contains("winter")) {
            condition = "Snow";
            description = "snow";
            temp = -5.0;
            humidity = 75;
            windSpeed = 8.5;
        } else if (normalizedLocation.contains("storm") || normalizedLocation.contains("thunder")) {
            condition = "Thunderstorm";
            description = "thunderstorm with rain";
            temp = 18.0;
            humidity = 90;
            windSpeed = 25.5;
        } else if (normalizedLocation.contains("cloud")) {
            condition = "Cloudy";
            description = "overcast clouds";
            temp = 20.0;
            humidity = 70;
            windSpeed = 6.5;
        } else {
            // For unrecognized locations, derive condition from seeded values.
            if (windSpeed > 18.0) {
                condition = "Windy";
                description = "gusty winds";
            } else if (humidity > 78 && temp < 22) {
                condition = "Cloudy";
                description = "overcast clouds";
            } else if (humidity > 72 && temp >= 22) {
                condition = "Rainy";
                description = "light rain";
            } else {
                condition = "Clear";
                description = temp > 30 ? "hot and sunny" : "clear sky";
            }
        }
        
        result.put("location", location);
        result.put("temperature", temp);
        double feelsLike = temp + ((humidity - 60) * 0.03) - (windSpeed * 0.08);
        result.put("feelsLike", Math.round(feelsLike * 10.0) / 10.0);
        result.put("humidity", humidity);
        result.put("windSpeed", Math.round(windSpeed * 10.0) / 10.0);
        result.put("condition", condition);
        result.put("description", description);
        result.put("success", true);
        
        return result;
    }
    
    /**
     * Check if weather is suitable for outdoor activities
     */
    public Map<String, Object> getWeatherAlert(Map<String, Object> weather) {
        Map<String, Object> alert = new HashMap<>();
        String condition = (String) weather.get("condition");
        double windSpeed = ((Number) weather.get("windSpeed")).doubleValue();
        double temp = ((Number) weather.get("temperature")).doubleValue();
        
        boolean hasSevereWarning = false;
        boolean hasWarning = false;
        String message = "Weather looks good for outdoor activities!";
        String severity = "info";
        
        // Check for severe weather warnings
        if ("Thunderstorm".equals(condition)) {
            hasSevereWarning = true;
            message = "⚠️ SEVERE: Thunderstorm expected! Reschedule outdoor tasks!";
            severity = "danger";
        } else if ("Snow".equals(condition) && temp < -5) {
            hasSevereWarning = true;
            message = "⚠️ SEVERE: Extreme cold with snow. Not suitable for outdoor activities!";
            severity = "danger";
        } else if (windSpeed > 20) {
            hasSevereWarning = true;
            message = "⚠️ SEVERE: Strong winds (" + windSpeed + " km/h). Consider rescheduling!";
            severity = "danger";
        }
        
        // Check for moderate warnings
        if (!hasSevereWarning) {
            if ("Rainy".equals(condition)) {
                hasWarning = true;
                message = "⚠️ WARNING: Rain expected. Bring umbrella or reschedule!";
                severity = "warning";
            } else if ("Snow".equals(condition)) {
                hasWarning = true;
                message = "⚠️ WARNING: Snow in forecast. Plan accordingly!";
                severity = "warning";
            } else if (temp > 35) {
                hasWarning = true;
                message = "⚠️ WARNING: Very hot (" + temp + "°C). Stay hydrated during outdoor tasks!";
                severity = "warning";
            } else if (temp < 0) {
                hasWarning = true;
                message = "⚠️ WARNING: Below freezing. Wear appropriate clothing!";
                severity = "warning";
            } else if (windSpeed > 15) {
                hasWarning = true;
                message = "⚠️ WARNING: Moderate to strong winds. Exercise caution!";
                severity = "warning";
            }
        }
        
        alert.put("hasSevereWarning", hasSevereWarning);
        alert.put("hasWarning", hasWarning);
        alert.put("message", message);
        alert.put("severity", severity);
        alert.put("suitable", !hasSevereWarning);
        
        return alert;
    }
}
