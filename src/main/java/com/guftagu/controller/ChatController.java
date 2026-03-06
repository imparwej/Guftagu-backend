package com.guftagu.controller;

import com.guftagu.model.ChatRoom;
import com.guftagu.model.Message;
import com.guftagu.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.guftagu.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ChatController {

    private final ChatService chatService;

    // Start chat
    @PostMapping("/start/{targetUserId}")
    public ResponseEntity<ChatRoom> startChat(@PathVariable String targetUserId, Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String currentUserId = principal.getUser().getId();

        return ResponseEntity.ok(chatService.startChat(currentUserId, targetUserId));
    }

    // Get chat rooms of user
    @GetMapping("/rooms/{userId}")
    public ResponseEntity<List<ChatRoom>> getChatRooms(@PathVariable String userId) {

        return ResponseEntity.ok(chatService.getChatRooms(userId));
    }

    // Get chat history
    @GetMapping("/history/{roomId}")
    public ResponseEntity<List<Message>> getChatHistory(@PathVariable String roomId) {

        return ResponseEntity.ok(chatService.getChatHistory(roomId));
    }
}