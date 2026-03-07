package com.guftagu.service;

import com.guftagu.dto.UserDTO;
import com.guftagu.model.User;
import com.guftagu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final UserRepository userRepository;

    public List<UserDTO> syncContacts(List<String> phoneNumbers) {

        return phoneNumbers
                .stream()
                .map(userRepository::findByPhoneNumber)
                .filter(optionalUser -> optionalUser.isPresent())
                .map(optionalUser -> mapToDTO(optionalUser.get()))
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