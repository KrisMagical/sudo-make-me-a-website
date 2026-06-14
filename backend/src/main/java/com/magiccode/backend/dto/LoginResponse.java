package com.magiccode.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType;   // "Bearer"
    private long expiresIn;     // 秒
    private String username;
    private String role;
}

