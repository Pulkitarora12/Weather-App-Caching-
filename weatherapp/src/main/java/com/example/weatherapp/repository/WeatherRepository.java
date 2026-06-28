package com.example.weatherapp.repository;

import com.example.weatherapp.model.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeatherRepository extends JpaRepository<WeatherData, Long> {
    Optional<WeatherData> findByCityIgnoreCase(String city);
}
