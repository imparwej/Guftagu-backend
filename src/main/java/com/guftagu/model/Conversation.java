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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    @Indexed
    private String user1Id;

    @Indexed
    private String user2Id;

    private String lastMessage;

    private LocalDateTime lastMessageTime;

    private int unreadCountUser1;

    private int unreadCountUser2;

    // Pinned chats — list of user IDs who have pinned this conversation
    @Builder.Default
    private List<String> pinnedBy = new ArrayList<>();
}
