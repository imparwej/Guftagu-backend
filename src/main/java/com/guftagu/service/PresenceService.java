package com.guftagu.service;

import com.guftagu.model.User;
import com.guftagu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    private static final String ONLINE_KEY_PREFIX = "online:";
    private static final long TTL_SECONDS = 60;

    public void setUserOnline(String userId) {
        redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, true, TTL_SECONDS, TimeUnit.SECONDS);
        
        // Ensure online status is reflected in db
        userRepository.findById(userId).ifPresent(user -> {
            if (!user.isOnlineStatus()) {
                user.setOnlineStatus(true);
                userRepository.save(user);
            }
        });
    }

    public void setUserOffline(String userId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
        
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnlineStatus(false);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public boolean isUserOnline(String userId) {
        Boolean isOnline = (Boolean) redisTemplate.opsForValue().get(ONLINE_KEY_PREFIX + userId);
        return Boolean.TRUE.equals(isOnline);
    }
}
