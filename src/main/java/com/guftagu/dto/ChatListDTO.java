package com.guftagu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatListDTO {
    private String chatId;
    private String userId;
    private String name;
    private String profilePicture;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
    private boolean isMuted;
    private boolean isPinned;
    private boolean isTyping;
}
