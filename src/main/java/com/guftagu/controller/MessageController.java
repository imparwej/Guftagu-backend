package com.guftagu.controller;

import com.guftagu.model.Message;
import com.guftagu.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    // WebSocket Endpoint
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Message chatMessage) {

        chatMessage.setDelivered(false);
        chatMessage.setSeen(false);

        // Save message using ChatService (which handles everything)
        Message savedMessage = chatService.saveMessage(chatMessage);

        // Deliver to receiver instantly via WebSocket
        // Destination: /topic/messages/{roomId}
        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getRoomId(), savedMessage);
    }

    // REST Endpoint
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<Message>> getMessagesByRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(chatService.getChatHistory(roomId));
    }
}
