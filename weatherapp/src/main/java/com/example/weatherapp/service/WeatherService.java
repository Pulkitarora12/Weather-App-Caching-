package com.example.weatherapp.service;

import com.example.weatherapp.model.WeatherData;
import com.example.weatherapp.repository.WeatherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private final WeatherRepository weatherRepository;

    @Autowired
    public WeatherService(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    public WeatherData saveWeather(WeatherData data) {
        logger.info("Database Call: findByCityIgnoreCase for city: {}", data.getCity());
        Optional<WeatherData> existing = weatherRepository.findByCityIgnoreCase(data.getCity());
        if (existing.isPresent()) {
            WeatherData existingData = existing.get();
            existingData.setTemperature(data.getTemperature());
            existingData.setDescription(data.getDescription());
            existingData.setHumidity(data.getHumidity());
            existingData.setWindSpeed(data.getWindSpeed());
            logger.info("Database Call: save (update) for city: {}", data.getCity());
            return weatherRepository.save(existingData);
        }
        logger.info("Database Call: save (create new) for city: {}", data.getCity());
        return weatherRepository.save(data);
    }

    @Cacheable(value = "weather", key = "#city")
    public Optional<WeatherData> getWeatherByCity(String city) {
        logger.info("Database Call: findByCityIgnoreCase for city: {}", city);
        return weatherRepository.findByCityIgnoreCase(city);
    }

    @Cacheable("allWeather")
    public List<WeatherData> getAllWeather() {
        logger.info("Database Call: findAll");
        return weatherRepository.findAll();
    }
    
    @CachePut(value = "weather", key = "#city")
    public WeatherData updateWeather(String city, WeatherData data) {
        logger.info("Database Call: findByCityIgnoreCase for update of city: {}", city);
        WeatherData existingData = weatherRepository.findByCityIgnoreCase(city)
                .orElseThrow(() -> new RuntimeException("Weather data not found for city: " + city));
        
        if (data.getTemperature() != null) {
            existingData.setTemperature(data.getTemperature());
        }
        if (data.getDescription() != null) {
            existingData.setDescription(data.getDescription());
        }
        if (data.getHumidity() != null) {
            existingData.setHumidity(data.getHumidity());
        }
        if (data.getWindSpeed() != null) {
            existingData.setWindSpeed(data.getWindSpeed());
        }
        logger.info("Database Call: save (update existing fields) for city: {}", city);
        return weatherRepository.save(existingData);
    }

    @CacheEvict(value = "weather", key = "#city")
    public boolean deleteWeather(String city) {
        logger.info("Database Call: findByCityIgnoreCase for deletion of city: {}", city);
        Optional<WeatherData> existing = weatherRepository.findByCityIgnoreCase(city);
        if (existing.isPresent()) {
            logger.info("Database Call: delete for city: {}", city);
            weatherRepository.delete(existing.get());
            return true;
        }
        return false;
    }
}
