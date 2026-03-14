package com.guftagu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TypingService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TYPING_KEY_PREFIX = "typing:";
    private static final long TTL_SECONDS = 5;

    public void setTypingStatus(String chatId, String userId) {
        if (userId == null) {
            redisTemplate.delete(TYPING_KEY_PREFIX + chatId);
        } else {
            redisTemplate.opsForValue().set(TYPING_KEY_PREFIX + chatId, userId, TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    public boolean isOtherUserTyping(String chatId, String currentUserId) {
        String typingUserId = (String) redisTemplate.opsForValue().get(TYPING_KEY_PREFIX + chatId);
        return typingUserId != null && !typingUserId.equals(currentUserId);
    }
}
