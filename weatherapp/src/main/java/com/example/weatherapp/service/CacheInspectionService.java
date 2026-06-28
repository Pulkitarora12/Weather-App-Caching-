package com.example.weatherapp.service;

import org.springframework.stereotype.Service;

import java.util.Objects;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@Service
public class CacheInspectionService {

    private final CacheManager cacheManager;

    public CacheInspectionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void printCacheContents(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            System.out.println("Cache Contents : ");
            System.out.println(Objects.requireNonNull(cache.getNativeCache()).toString());
        } else {
            System.out.println("No such cache: " + cacheName);
        }
    }

    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            System.out.println("Cleared cache: " + cacheName);
        }
    }
}
