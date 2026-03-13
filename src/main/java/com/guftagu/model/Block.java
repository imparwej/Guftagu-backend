package com.guftagu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "blocks")
@CompoundIndexes({
    @CompoundIndex(name = "blocker_blocked_idx", def = "{'blockerId': 1, 'blockedUserId': 1}", unique = true)
})
public class Block {

    @Id
    private String id;

    @Indexed
    private String blockerId;

    @Indexed
    private String blockedUserId;

    private LocalDateTime createdAt;
}
