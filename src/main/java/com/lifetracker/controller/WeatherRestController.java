package com.lifetracker.controller;

import com.lifetracker.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherRestController {
    
    private final WeatherService weatherService;
    
    @GetMapping("/location")
    public Map<String, Object> getWeatherByLocation(@RequestParam String location) {
        return weatherService.getWeatherByLocation(location);
    }
    
    @PostMapping("/alert")
    public Map<String, Object> getWeatherAlert(@RequestBody Map<String, Object> weather) {
        return weatherService.getWeatherAlert(weather);
    }
}
