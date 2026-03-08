package com.guftagu.controller;

import com.guftagu.dto.UserDTO;
import com.guftagu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin("*")
public class UserController {

    private final UserService userService;

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
    public ResponseEntity<?> blockUser(@RequestBody Map<String, String> request) {
        String blockerId = request.get("blockerId");
        String blockedId = request.get("blockedId");
        userService.blockUser(blockerId, blockedId);
        return ResponseEntity.ok(Map.of("blocked", true));
    }

    @PostMapping("/unblock")
    public ResponseEntity<?> unblockUser(@RequestBody Map<String, String> request) {
        String blockerId = request.get("blockerId");
        String blockedId = request.get("blockedId");
        userService.unblockUser(blockerId, blockedId);
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
}
