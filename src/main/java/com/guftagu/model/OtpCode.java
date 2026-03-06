package com.guftagu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp_codes")
public class OtpCode {

    @Id
    private String id;
    
    private String phoneNumber;
    
    private String otpCode;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime expiresAt;
}
