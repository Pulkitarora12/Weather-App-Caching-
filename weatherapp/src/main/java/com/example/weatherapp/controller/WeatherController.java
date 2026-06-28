package com.example.weatherapp.controller;

import com.example.weatherapp.model.WeatherData;
import com.example.weatherapp.service.WeatherService;
import com.example.weatherapp.service.CacheInspectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;
    private final CacheInspectionService cacheInspectionService;

    public WeatherController(WeatherService weatherService, CacheInspectionService cacheInspectionService) {
        this.weatherService = weatherService;
        this.cacheInspectionService = cacheInspectionService;
    }

    // Input/Save weather data
    @PostMapping
    public ResponseEntity<WeatherData> saveWeather(@RequestBody WeatherData weatherData) {
        WeatherData saved = weatherService.saveWeather(weatherData);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // Get all weather data
    @GetMapping
    public ResponseEntity<List<WeatherData>> getAllWeather() {
        List<WeatherData> allWeather = weatherService.getAllWeather();
        return ResponseEntity.ok(allWeather);
    }

    // Get weather data by city
    @GetMapping("/{city}")
    public ResponseEntity<WeatherData> getWeatherByCity(@PathVariable String city) {
        return weatherService.getWeatherByCity(city)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update weather data by city
    @PutMapping("/{city}")
    public ResponseEntity<WeatherData> updateWeather(@PathVariable String city, @RequestBody WeatherData weatherData) {
        try {
            WeatherData updated = weatherService.updateWeather(city, weatherData);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete weather data by city
    @DeleteMapping("/{city}")
    public ResponseEntity<Void> deleteWeather(@PathVariable String city) {
        boolean deleted = weatherService.deleteWeather(city);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/getCacheData")
    public void getCacheData() {
        cacheInspectionService.printCacheContents("weather");
    }

    @GetMapping("/clearCache")
    public String clearCache() {
        cacheInspectionService.clearCache("weather");
        return "Cache cleared successfully!";
    }
}
