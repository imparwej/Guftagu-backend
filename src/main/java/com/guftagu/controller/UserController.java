package com.guftagu.controller;

import com.guftagu.dto.UserDTO;
import com.guftagu.service.UserService;
import com.guftagu.service.BlockService;
import com.guftagu.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users") // We keep existing /users mapped, but clients can call /api/users mapping similarly if proxy exists
@RequiredArgsConstructor
@CrossOrigin("*")
public class UserController {

    private final UserService userService;
    private final BlockService blockService;
    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }

    @PostMapping("/block")
    public ResponseEntity<?> blockUserLegacy(@RequestBody Map<String, String> request) {
        String blockerId = request.get("blockerId");
        String blockedId = request.get("blockedId");
        userService.blockUser(blockerId, blockedId);
        blockService.blockUser(blockerId, blockedId); // Sync with new Block domain
        return ResponseEntity.ok(Map.of("blocked", true));
    }

    @PostMapping("/unblock")
    public ResponseEntity<?> unblockUserLegacy(@RequestBody Map<String, String> request) {
        String blockerId = request.get("blockerId");
        String blockedId = request.get("blockedId");
        userService.unblockUser(blockerId, blockedId);
        blockService.unblockUser(blockerId, blockedId); // Sync with new Block domain
        return ResponseEntity.ok(Map.of("blocked", false));
    }



    @PostMapping("/mute")
    public ResponseEntity<?> toggleMute(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String conversationId = request.get("conversationId");
        String muteDuration = request.get("muteDuration"); // Currently logged/passed but logic remains toggle
        boolean isMuted = userService.toggleMuteConversation(userId, conversationId, muteDuration);
        return ResponseEntity.ok(Map.of("muted", isMuted));
    }

    /**
     * Store or update the device FCM token for push notifications.
     */
    @PostMapping("/device-token")
    public ResponseEntity<?> updateDeviceToken(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String token = request.get("token");
        userService.updateDeviceToken(userId, token);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Store or update the user's RSA public key for E2EE.
     */
    @PostMapping("/public-key")
    public ResponseEntity<?> updatePublicKey(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String publicKey = request.get("publicKey");
        System.out.println("[UserController] Updating public key for user: " + userId);
        userService.updatePublicKey(userId, publicKey);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Fetch the RSA public key of a specific user.
     */
    @GetMapping("/{id}/public-key")
    public ResponseEntity<?> getPublicKey(@PathVariable String id) {
        System.out.println("[UserController] Fetching public key for user: " + id);
        // First check if user exists
        try {
            userService.getUserById(id);
        } catch (Exception e) {
            System.out.println("[UserController] User not found: " + id);
            return ResponseEntity.notFound().build();
        }

        String publicKey = userService.getPublicKey(id);
        if (publicKey == null) {
            System.out.println("[UserController] Public key is NULL for user: " + id + ". Returning empty string.");
            return ResponseEntity.ok(Map.of("publicKey", ""));
        }
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }
}
