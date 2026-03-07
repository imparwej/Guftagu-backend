package com.guftagu.controller;

import com.guftagu.dto.*;
import com.guftagu.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest request) {
        authService.sendOtp(request.getPhoneNumber());
        return ResponseEntity.ok().body(AuthResponse.builder().status("OTP_SENT").build());
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody OtpVerify request) {
        return ResponseEntity.ok(authService.verifyOtp(request.getPhoneNumber(), request.getOtp()));
    }

    @PostMapping("/create-profile")
    public ResponseEntity<AuthResponse> createProfile(@RequestBody CreateProfileRequest request) {
        return ResponseEntity.ok(authService.createProfile(request));
    }
}
