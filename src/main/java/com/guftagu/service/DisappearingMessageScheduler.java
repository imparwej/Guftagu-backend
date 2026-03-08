package com.guftagu.service;

import com.guftagu.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisappearingMessageScheduler {

    private final MessageRepository messageRepository;

    /**
     * Runs every 5 minutes — deletes messages whose expiresAt timestamp has passed.
     */
    @Scheduled(fixedRate = 300000)
    public void deleteExpiredMessages() {
        long now = System.currentTimeMillis();
        long count = messageRepository.deleteByExpiresAtNotNullAndExpiresAtLessThan(now);
        if (count > 0) {
            log.info("Deleted {} expired disappearing messages", count);
        }
    }
}
