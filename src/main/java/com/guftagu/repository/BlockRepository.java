package com.guftagu.repository;

import com.guftagu.model.Block;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface BlockRepository extends MongoRepository<Block, String> {
    Optional<Block> findByBlockerIdAndBlockedUserId(String blockerId, String blockedUserId);
    boolean existsByBlockerIdAndBlockedUserId(String blockerId, String blockedUserId);
    void deleteByBlockerIdAndBlockedUserId(String blockerId, String blockedUserId);
}
