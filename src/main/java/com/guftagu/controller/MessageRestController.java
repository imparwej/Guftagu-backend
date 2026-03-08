package com.guftagu.controller;

import com.guftagu.model.Message;
import com.guftagu.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MessageRestController {

    private final MessageService messageService;

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
}
