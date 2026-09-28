package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Admin login request.")
public class LoginRequest {
    @Schema(description = "Admin username.", example = "admin")
    private String username;

    @Schema(description = "Admin password. The example is not a default password.", example = "ChangeMe_StrongPassword_Example", format = "password")
    private String password;
}
