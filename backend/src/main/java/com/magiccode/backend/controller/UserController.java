package com.magiccode.backend.controller;

import com.magiccode.backend.dto.LoginRequest;
import com.magiccode.backend.dto.LoginResponse;
import com.magiccode.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.magiccode.backend.dto.ApiErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Auth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Admin login", description = "Authenticates an admin user and returns a bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login succeeded"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }
}
