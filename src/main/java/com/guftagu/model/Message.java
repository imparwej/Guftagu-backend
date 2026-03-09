package com.guftagu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
public class Message {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    @Indexed
    private String senderId;

    @Indexed
    private String receiverId;

    private MessageType type;

    private String content;

    private String mediaUrl;

    @Indexed
    private long timestamp;

    private boolean delivered;

    private boolean seen;

    // Link preview metadata (title, description, image, siteName, url)
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    // Disappearing messages — epoch millis when message expires (null = never)
    @Indexed
    private Long expiresAt;

    // Clear chat per user — list of user IDs who have deleted this message locally
    @Builder.Default
    private List<String> deletedFor = new ArrayList<>();

    // Delete for everyone flag
    @Builder.Default
    private boolean deletedForEveryone = false;

    // Star message flag
    @Builder.Default
    private boolean starred = false;

    // Forward message flag
    @Builder.Default
    private boolean forwarded = false;

    // Reactions — userId -> emoji (only one reaction per user)
    @Builder.Default
    private Map<String, String> reactions = new HashMap<>();

    // Message editing
    @Builder.Default
    private boolean edited = false;

    private Long editedAt;
}
