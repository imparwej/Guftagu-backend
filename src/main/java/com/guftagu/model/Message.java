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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "chat_ts_idx", def = "{'conversationId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "chat_recv_seen_idx", def = "{'conversationId': 1, 'receiverId': 1, 'seen': 1}")
})
public class Message {

    @Id
    private String id;
    private String senderId;
    private String receiverId;
    private String conversationId;
    private String content;
    private MessageType type;
    private String mediaUrl;
    private String thumbnailUrl;
    private long timestamp;
    private boolean delivered;
    private boolean seen;
    private String fileName;
    private Long fileSize;
    private Double voiceDuration;
    
    // End-to-End Encryption fields
    private String encryptedMessage;
    private String encryptedAESKey;
    @Builder.Default
    private boolean isEncrypted = false;

    // Additional fields for extended features
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    @Indexed
    private Long expiresAt;
    @Builder.Default
    private List<String> deletedFor = new ArrayList<>();
    @Builder.Default
    private boolean deletedForEveryone = false;
    @Builder.Default
    private boolean starred = false;
    @Builder.Default
    private boolean forwarded = false;
    @Builder.Default
    private Map<String, String> reactions = new HashMap<>();
    @Builder.Default
    private boolean edited = false;
    private Long editedAt;
}
