package com.guftagu.service;

import com.guftagu.dto.UserDTO;
import com.guftagu.model.User;
import com.guftagu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private UserDTO mapToDTO(User user) {

        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .onlineStatus(user.isOnlineStatus())
                .lastSeen(user.getLastSeen())
                .createdAt(user.getCreatedAt())
                .build();
    }
}