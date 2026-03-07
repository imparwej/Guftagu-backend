package com.guftagu.service;

import com.guftagu.dto.*;
import com.guftagu.model.User;
import com.guftagu.repository.UserRepository;
import com.guftagu.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    private static class OtpData {
        private String otp;
        private LocalDateTime expiry;

        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        public LocalDateTime getExpiry() { return expiry; }
        public void setExpiry(LocalDateTime expiry) { this.expiry = expiry; }
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";

        String normalized = phoneNumber
                .replaceAll("\\s+", "")
                .trim();

        if (!normalized.startsWith("+")) {
            if (normalized.length() == 10) {
                normalized = "+91" + normalized;
            } else if (normalized.startsWith("91") && normalized.length() == 12) {
                normalized = "+" + normalized;
            }
        }
        return normalized;
    }

    public void sendOtp(String phoneNumber) {
        String normalizedNumber = normalizePhoneNumber(phoneNumber);
        String otp = String.format("%06d", new Random().nextInt(1000000));
        
        OtpData otpData = new OtpData();
        otpData.setOtp(otp);
        otpData.setExpiry(LocalDateTime.now().plusMinutes(5));

        otpStorage.put(normalizedNumber, otpData);
        
        System.out.println("OTP Stored = " + otpData.getOtp());
        System.out.println("OTP for " + normalizedNumber + " = " + otp);
    }

    public AuthResponse verifyOtp(String phoneNumber, String otp) {
        String normalizedNumber = normalizePhoneNumber(phoneNumber);
        OtpData otpData = otpStorage.get(normalizedNumber);
        
        String enteredOtp = otp == null ? "" : otp.trim();
        String storedOtp = otpData != null ? otpData.getOtp() : null;

        System.out.println("OTP Stored = " + storedOtp);
        System.out.println("OTP Entered = " + enteredOtp);

        if (storedOtp == null) {
            return AuthResponse.builder()
                    .status("OTP_EXPIRED")
                    .build();
        }

        if (!storedOtp.equals(enteredOtp)) {
            return AuthResponse.builder()
                    .status("INVALID_OTP")
                    .build();
        }

        otpStorage.remove(normalizedNumber);

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
                .profilePicture(request.getProfilePicture())
                .onlineStatus(false)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(savedUser.getPhoneNumber())
                .password("")
                .authorities("USER")
                .build();

        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .status("PROFILE_CREATED")
                .token(token)
                .user(savedUser)
                .build();
    }
}
