package com.guftagu.controller;

import com.guftagu.dto.ChatListDTO;
import com.guftagu.model.Message;
import com.guftagu.security.UserPrincipal;
import com.guftagu.service.ChatService;
import com.guftagu.service.MessageService;
import com.guftagu.service.UserService;
import com.guftagu.service.BlockService;
import com.guftagu.dto.UserDTO;
import com.guftagu.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ApiChatController {

    private final MessageService messageService;
    private final ChatService chatService;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final BlockService blockService;
    private final SimpMessagingTemplate messagingTemplate;

    // STEP 2 - Chat List
    @GetMapping("/chats")
    public ResponseEntity<List<ChatListDTO>> getChats(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return ResponseEntity.status(401).build();
        }
        String userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
        return ResponseEntity.ok(chatService.getChats(userId));
    }

    // STEP 4 - Pin Chat
    @PostMapping("/chat/{chatId}/pin")
    public ResponseEntity<?> pinChat(@PathVariable String chatId, Authentication authentication) {
        String userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
        chatService.pinChat(chatId, userId);
        return ResponseEntity.ok(Map.of("pinned", true));
    }

    @PostMapping("/chat/{chatId}/unpin")
    public ResponseEntity<?> unpinChat(@PathVariable String chatId, Authentication authentication) {
        String userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
        chatService.unpinChat(chatId, userId);
        return ResponseEntity.ok(Map.of("pinned", false));
    }

    // STEP 5 - Mute Chat
    @PostMapping("/chat/{chatId}/mute")
    public ResponseEntity<?> muteChat(@PathVariable String chatId, Authentication authentication) {
        String userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
        chatService.muteChat(chatId, userId);
        messageService.muteChat(chatId, userId); // Keep legacy sync if needed
        return ResponseEntity.ok(Map.of("muted", true));
    }

    @PostMapping("/chat/{chatId}/unmute")
    public ResponseEntity<?> unmuteChat(@PathVariable String chatId, Authentication authentication) {
        String userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
        chatService.unmuteChat(chatId, userId);
        messageService.unmuteChat(chatId, userId); // Keep legacy sync if needed
        return ResponseEntity.ok(Map.of("muted", false));
    }

    // STEP 6 - Mark as Read
    @PostMapping("/chat/{chatId}/mark-read")
    public ResponseEntity<?> markAsRead(@PathVariable String chatId, Authentication authentication) {
        String userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
        chatService.markMessagesAsRead(chatId, userId);
        
        messagingTemplate.convertAndSend("/topic/read-receipts/" + chatId, Map.of(
            "chatId", chatId,
            "readBy", userId,
            "readAt", java.time.LocalDateTime.now()
        ));
        
        return ResponseEntity.ok(Map.of("read", true));
    }

    // STEP 7 - Delete Chat for Context User
    @DeleteMapping("/chat/{chatId}")
    public ResponseEntity<?> deleteChat(@PathVariable String chatId, Authentication authentication) {
        String userId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
        chatService.deleteChat(chatId, userId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    /**
     * Clear all messages in a chat (deletes from DB), but keeps the chat document.
     */
    @DeleteMapping("/chat/{chatId}/clear")
    public ResponseEntity<?> clearChatAll(@PathVariable String chatId) {
        try {
            messageRepository.deleteByChatId(chatId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to fully clear chat: chatId={}", chatId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    /**
     * Find Media by Chat ID (Images & Videos)
     */
    @GetMapping("/chat/{chatId}/media")
    public ResponseEntity<List<Message>> getMedia(@PathVariable String chatId) {
        return ResponseEntity.ok(messageService.getMedia(chatId));
    }

    /**
     * Find Links by Chat ID
     */
    @GetMapping("/chat/{chatId}/links")
    public ResponseEntity<List<Message>> getLinks(@PathVariable String chatId) {
        return ResponseEntity.ok(messageService.getLinks(chatId));
    }

    /**
     * Find Docs by Chat ID (Documents / Files)
     */
    @GetMapping("/chat/{chatId}/docs")
    public ResponseEntity<List<Message>> getDocs(@PathVariable String chatId) {
        return ResponseEntity.ok(messageService.getDocs(chatId));
    }

    // STEP 8 & 9 - View Contact & Block User
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<?> blockUserRoute(@PathVariable String userId, Authentication authentication) {
        String blockerId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
        blockService.blockUser(blockerId, userId);
        userService.blockUser(blockerId, userId);
        return ResponseEntity.ok(Map.of("blocked", true));
    }

    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<?> unblockUserRoute(@PathVariable String userId, Authentication authentication) {
        String blockerId = ((UserPrincipal) authentication.getPrincipal()).getUser().getId();
        blockService.unblockUser(blockerId, userId);
        userService.unblockUser(blockerId, userId);
        return ResponseEntity.ok(Map.of("blocked", false));
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<?> getUserProfile(@PathVariable String userId, @RequestParam(required = false) String chatId) {
        UserDTO user = userService.getUserById(userId);
        long mediaCount = 0;
        long docsCount = 0;
        long linksCount = 0;

        if (chatId != null && !chatId.isEmpty()) {
            mediaCount = messageService.getMedia(chatId).size();
            docsCount = messageService.getDocs(chatId).size();
            linksCount = messageService.getLinks(chatId).size();
        }

        return ResponseEntity.ok(Map.of(
                "name", user.getName() != null ? user.getName() : "Unknown",
                "profilePicture", user.getProfilePicture() != null ? user.getProfilePicture() : "",
                "status", user.getBio() != null ? user.getBio() : "",
                "mediaCount", mediaCount,
                "docsCount", docsCount,
                "linksCount", linksCount
        ));
    }
}
