package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Successful admin login response.")
public class LoginResponse {
    @Schema(description = "JWT access token. Use it as Authorization: Bearer <token>.", example = "eyJhbGciOiJIUzI1NiJ9.example.signature")
    private String token;

    @Schema(description = "Token type.", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token lifetime in seconds.", example = "86400")
    private long expiresIn;

    @Schema(description = "Authenticated username.", example = "admin")
    private String username;

    @Schema(description = "Authenticated role.", example = "ROOT")
    private String role;
}
