package com.guftagu.controller;

import com.guftagu.dto.ConversationResponse;
import com.guftagu.model.Message;
import com.guftagu.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ChatRestController {

    private final MessageService messageService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<ConversationResponse>> getConversations(@PathVariable String userId) {
        return ResponseEntity.ok(messageService.getChatList(userId));
    }

    // Message history moved to MessageRestController as per STEP 7

    @PostMapping("/messages/read")
    public ResponseEntity<?> markAsRead(@RequestBody Map<String, String> request) {
        String conversationId = request.get("conversationId");
        String userId = request.get("userId");
        try {
            messageService.markMessagesAsRead(conversationId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to mark messages as read: conversationId={}, userId={}", conversationId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * Clear chat for a specific user (only hides messages for this user).
     */
    @DeleteMapping("/{conversationId}/clear/{userId}")
    public ResponseEntity<?> clearChat(
            @PathVariable String conversationId,
            @PathVariable String userId) {
        try {
            messageService.clearChatForUser(conversationId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to clear chat: conversationId={}, userId={}", conversationId, userId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * Clear all messages in a chat (deletes from DB).
     */
    @DeleteMapping(value = { "/{conversationId}/clear", "/chat/{conversationId}/clear" })
    public ResponseEntity<?> clearChatAll(@PathVariable String conversationId) {
        try {
            messageService.clearChat(conversationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to fully clear chat: conversationId={}", conversationId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * Mute notifications for a specific chat.
     */
    @PostMapping(value = { "/{conversationId}/mute", "/chat/{conversationId}/mute" })
    public ResponseEntity<?> muteChat(@PathVariable String conversationId, @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }
        messageService.muteChat(conversationId, userId);
        return ResponseEntity.ok(Map.of("muted", true));
    }

    /**
     * Unmute notifications for a specific chat.
     */
    @PostMapping(value = { "/{conversationId}/unmute", "/chat/{conversationId}/unmute" })
    public ResponseEntity<?> unmuteChat(@PathVariable String conversationId, @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }
        messageService.unmuteChat(conversationId, userId);
        return ResponseEntity.ok(Map.of("muted", false));
    }


    /**
     * Get media/docs/links for a conversation.
     * Query param 'category' = media | docs | links | all
     */
    @GetMapping("/{conversationId}/media")
    public ResponseEntity<List<Message>> getMediaMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "all") String category) {
        return ResponseEntity.ok(messageService.getMediaMessages(conversationId, category));
    }

    /**
     * Toggle pin status for a conversation.
     */
    @PostMapping("/pin")
    public ResponseEntity<?> togglePin(@RequestBody Map<String, String> request) {
        String conversationId = request.get("conversationId");
        String userId = request.get("userId");
        boolean isPinned = messageService.togglePin(conversationId, userId);
        return ResponseEntity.ok(Map.of("pinned", isPinned));
    }
}

