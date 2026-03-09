package com.guftagu.controller;

import com.guftagu.model.Message;
import com.guftagu.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MessageRestController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/{conversationId}")
    public ResponseEntity<List<Message>> getMessageHistory(
            @PathVariable String conversationId,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(messageService.getMessageHistory(conversationId, userId));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Message> deleteMessage(
            @PathVariable String messageId,
            @RequestParam String userId,
            @RequestParam boolean forEveryone) {
        Message updatedMessage = messageService.deleteMessage(messageId, userId, forEveryone);
        if (updatedMessage != null) {
            return ResponseEntity.ok(updatedMessage);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{messageId}/star")
    public ResponseEntity<Message> toggleStar(@PathVariable String messageId) {
        Message updatedMessage = messageService.toggleMessageStar(messageId);
        if (updatedMessage != null) {
            return ResponseEntity.ok(updatedMessage);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/starred/{userId}")
    public ResponseEntity<List<Message>> getStarredMessages(@PathVariable String userId) {
        return ResponseEntity.ok(messageService.getStarredMessages(userId));
    }

    /**
     * React to a message. One reaction per user.
     */
    @PostMapping("/react")
    public ResponseEntity<?> reactToMessage(@RequestBody Map<String, String> request) {
        String messageId = request.get("messageId");
        String userId = request.get("userId");
        String reaction = request.get("reaction");
        Message updated = messageService.reactToMessage(messageId, userId, reaction);
        if (updated != null) {
            // Broadcast to both users
            messagingTemplate.convertAndSendToUser(updated.getSenderId(), "/queue/messages", updated);
            messagingTemplate.convertAndSendToUser(updated.getReceiverId(), "/queue/messages", updated);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Edit a message (within 15 minutes).
     */
    @PutMapping("/edit")
    public ResponseEntity<?> editMessage(@RequestBody Map<String, String> request) {
        String messageId = request.get("messageId");
        String newContent = request.get("newContent");
        Message updated = messageService.editMessage(messageId, newContent);
        if (updated != null) {
            // Broadcast edited message to both users
            messagingTemplate.convertAndSendToUser(updated.getSenderId(), "/queue/messages", updated);
            messagingTemplate.convertAndSendToUser(updated.getReceiverId(), "/queue/messages", updated);
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Cannot edit message. Time limit exceeded or message not found."));
    }
}

