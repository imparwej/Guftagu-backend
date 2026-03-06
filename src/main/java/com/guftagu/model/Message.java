package com.guftagu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
public class Message {

    @Id
    private String id;
    
    private String senderId;
    
    private String receiverId;
    
    private String roomId;
    
    private String message;
    
    private String messageType; // e.g., TEXT, IMAGE
    
    private LocalDateTime timestamp;
    
    private boolean delivered;
    
    private boolean seen;
}
