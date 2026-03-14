package com.guftagu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void updateLiveLocation(String chatId, String senderId, Map<String, Object> payload) {
        String key = "live_location:" + chatId + ":" + senderId;
        
        Object expiresAtObj = payload.get("expiresAt");
        long expiresAt = 0;
        if (expiresAtObj instanceof Number) {
            expiresAt = ((Number) expiresAtObj).longValue();
        }

        // Store temporary live location
        long ttlSeconds = 60; // Default TTL for the cache (frequent updates)
        if (expiresAt > 0) {
            long remaining = expiresAt - System.currentTimeMillis();
            if (remaining > 0) {
                ttlSeconds = remaining / 1000;
            }
        }
        
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(key, payload, ttlSeconds, TimeUnit.SECONDS);
        }
    }
}
