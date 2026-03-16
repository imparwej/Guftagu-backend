package com.guftagu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdate {
    private String conversationId;
    private String userId;
    private String senderId; // Including both for compatibility
    private double latitude;
    private double longitude;
    private long expiresAt;
    private String type; // Always LIVE_LOCATION
    private String senderName;
    private String senderAvatar;
}
