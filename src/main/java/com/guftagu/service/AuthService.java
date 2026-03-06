package com.guftagu.service;

import com.guftagu.dto.*;
import com.guftagu.model.User;
import com.guftagu.repository.UserRepository;
import com.guftagu.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ConcurrentHashMap<String, String> otpStore = new ConcurrentHashMap<>();

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        String normalized = phoneNumber.trim();
        if (normalized.startsWith("+91")) {
            return normalized;
        } else if (normalized.startsWith("91") && normalized.length() == 12) {
            return "+" + normalized;
        } else if (normalized.length() == 10) {
            return "+91" + normalized;
        }
        return normalized;
    }

    public void sendOtp(String phoneNumber) {
        String normalizedNumber = normalizePhoneNumber(phoneNumber);
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        
        otpStore.put(normalizedNumber, otp);
        
        System.out.println("OTP for " + normalizedNumber + " = " + otp);
    }

    public AuthResponse verifyOtp(String phoneNumber, String otp) {
        String normalizedNumber = normalizePhoneNumber(phoneNumber);
        
        if (!otpStore.containsKey(normalizedNumber)) {
            return AuthResponse.builder()
                    .status("OTP_EXPIRED")
                    .build();
        }

        if (!otpStore.get(normalizedNumber).equals(otp)) {
            return AuthResponse.builder()
                    .status("INVALID_OTP")
                    .build();
        }

        otpStore.remove(normalizedNumber);

        return userRepository.findByPhoneNumber(normalizedNumber)
                .map(user -> {
                    UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(user.getPhoneNumber())
                            .password("")
                            .authorities("USER")
                            .build();
                    String token = jwtUtil.generateToken(userDetails);
                    return AuthResponse.builder()
                            .status("LOGIN_SUCCESS")
                            .token(token)
                            .user(user)
                            .build();
                })
                .orElse(AuthResponse.builder()
                        .status("VERIFIED")
                        .build());
    }

    public AuthResponse createProfile(CreateProfileRequest request) {
        String normalizedNumber = normalizePhoneNumber(request.getPhoneNumber());
        
        if (userRepository.findByPhoneNumber(normalizedNumber).isPresent()) {
            throw new RuntimeException("User already exists with this phone number");
        }

        User user = User.builder()
                .phoneNumber(normalizedNumber)
                .name(request.getName())
                .bio(request.getBio())
                .profilePicture(request.getProfileImage())
                .onlineStatus(true)
                .lastSeen(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(savedUser.getPhoneNumber())
                .password("")
                .authorities("USER")
                .build();

        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .status("LOGIN_SUCCESS")
                .token(token)
                .user(savedUser)
                .build();
    }
}
