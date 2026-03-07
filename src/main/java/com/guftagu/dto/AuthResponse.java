package com.guftagu.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.guftagu.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String status;
    private String token;
    private User user;
}
