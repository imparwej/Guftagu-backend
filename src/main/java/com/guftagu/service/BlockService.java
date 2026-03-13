package com.guftagu.service;

import com.guftagu.model.Block;
import com.guftagu.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;

    public void blockUser(String blockerId, String blockedUserId) {
        if (!blockRepository.existsByBlockerIdAndBlockedUserId(blockerId, blockedUserId)) {
            Block block = Block.builder()
                    .blockerId(blockerId)
                    .blockedUserId(blockedUserId)
                    .createdAt(LocalDateTime.now())
                    .build();
            blockRepository.save(block);
        }
    }

    public void unblockUser(String blockerId, String blockedUserId) {
        blockRepository.deleteByBlockerIdAndBlockedUserId(blockerId, blockedUserId);
    }

    public boolean isBlocked(String blockerId, String blockedUserId) {
        return blockRepository.existsByBlockerIdAndBlockedUserId(blockerId, blockedUserId);
    }
}
