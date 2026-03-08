package com.guftagu.service;

import com.guftagu.dto.ContactDTO;
import com.guftagu.dto.ContactSyncResponse;
import com.guftagu.dto.UserDTO;
import com.guftagu.model.User;
import com.guftagu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final UserRepository userRepository;

    public ContactSyncResponse syncContacts(List<ContactDTO> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return ContactSyncResponse.builder()
                    .guftaguUsers(new ArrayList<>())
                    .inviteContacts(new ArrayList<>())
                    .build();
        }

        // Get current user's phone number to exclude
        String currentUserPhone = SecurityContextHolder.getContext().getAuthentication().getName();

        // Normalize and remove duplicates/current user
        Map<String, String> normalizedToOriginal = new HashMap<>();
        for (ContactDTO contact : contacts) {
            String normalized = normalizePhone(contact.getPhoneNumber());
            if (normalized != null && !normalized.equals(currentUserPhone)) {
                normalizedToOriginal.putIfAbsent(normalized, contact.getName());
            }
        }

        List<String> phoneNumbers = new ArrayList<>(normalizedToOriginal.keySet());
        List<User> foundUsers = userRepository.findByPhoneNumberIn(phoneNumbers);

        Set<String> guftaguPhoneNumbers = foundUsers.stream()
                .map(User::getPhoneNumber)
                .collect(Collectors.toSet());

        List<UserDTO> guftaguUsers = foundUsers.stream()
                .map(user -> {
                    UserDTO dto = mapToDTO(user);
                    // Use the contact name from the device if available
                    String deviceName = normalizedToOriginal.get(user.getPhoneNumber());
                    if (deviceName != null && !deviceName.isEmpty()) {
                        dto.setName(deviceName);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        List<ContactDTO> inviteContacts = normalizedToOriginal.entrySet().stream()
                .filter(entry -> !guftaguPhoneNumbers.contains(entry.getKey()))
                .map(entry -> ContactDTO.builder()
                        .name(entry.getValue())
                        .phoneNumber(entry.getKey())
                        .build())
                .collect(Collectors.toList());

        return ContactSyncResponse.builder()
                .guftaguUsers(guftaguUsers)
                .inviteContacts(inviteContacts)
                .build();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        // Basic normalization: remove spaces, dashes, parentheses
        // For a more advanced version, use libphonenumber
        String normalized = phone.replaceAll("[^0-9+]", "");
        if (normalized.isEmpty()) return null;
        return normalized;
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
}