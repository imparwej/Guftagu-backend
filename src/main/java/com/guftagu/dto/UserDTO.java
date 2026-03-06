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
public class UserDTO {

    private String id;
    private String name;
    private String phoneNumber;
    private String profilePicture;

    private boolean onlineStatus;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;

}