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
}
