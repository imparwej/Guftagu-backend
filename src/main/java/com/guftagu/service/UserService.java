package com.guftagu.service;

import com.guftagu.dto.UserDTO;
import com.guftagu.model.User;
import com.guftagu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserDTO> syncContacts(List<String> phoneNumbers) {

        List<User> registeredUsers = userRepository.findByPhoneNumberIn(phoneNumbers);

        return registeredUsers
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void updateOnlineStatus(String phoneNumber, boolean status) {

        userRepository.findByPhoneNumber(phoneNumber).ifPresent(user -> {

            user.setOnlineStatus(status);

            if (!status) {
                user.setLastSeen(LocalDateTime.now());
            }

            userRepository.save(user);
        });
    }

    public List<UserDTO> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(String id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToDTO(user);
    }

    public List<UserDTO> searchUsers(String query) {

        return userRepository
                .findByNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(query, query)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Block a user.
     */
    public void blockUser(String blockerId, String blockedId) {
        userRepository.findById(blockerId).ifPresent(user -> {
            if (user.getBlockedUsers() == null) {
                user.setBlockedUsers(new ArrayList<>());
            }
            if (!user.getBlockedUsers().contains(blockedId)) {
                user.getBlockedUsers().add(blockedId);
                userRepository.save(user);
            }
        });
    }

    /**
     * Unblock a user.
     */
    public void unblockUser(String blockerId, String blockedId) {
        userRepository.findById(blockerId).ifPresent(user -> {
            if (user.getBlockedUsers() != null) {
                user.getBlockedUsers().remove(blockedId);
                userRepository.save(user);
            }
        });
    }

    /**
     * Toggle mute for a conversation.
     */
    public boolean toggleMuteConversation(String userId, String conversationId, String muteDuration) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getMutedConversations() == null) {
            user.setMutedConversations(new ArrayList<>());
        }

        boolean isMuted;
        if (user.getMutedConversations().contains(conversationId)) {
            user.getMutedConversations().remove(conversationId);
            isMuted = false;
        } else {
            user.getMutedConversations().add(conversationId);
            isMuted = true;
        }
        userRepository.save(user);
        return isMuted;
    }

    /**
     * Check if a conversation is muted for a user.
     */
    public boolean isConversationMuted(String userId, String conversationId) {
        return userRepository.findById(userId)
                .map(user -> user.getMutedConversations() != null &&
                        user.getMutedConversations().contains(conversationId))
                .orElse(false);
    }

    private UserDTO mapToDTO(User user) {

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .bio(user.getBio())
                .onlineStatus(user.isOnlineStatus())
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Update device token for push notifications.
     */
    public void updateDeviceToken(String userId, String token) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setDeviceToken(token);
            userRepository.save(user);
        });
    }

    /**
     * Get device token for a user.
     */
    public String getDeviceToken(String userId) {
        return userRepository.findById(userId)
                .map(User::getDeviceToken)
                .orElse(null);
    }
}