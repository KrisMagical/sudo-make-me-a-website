package com.magiccode.backend.controller;

import com.magiccode.backend.config.OpenApiConfig;
import com.magiccode.backend.model.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Admin Auth")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminAuthController {

    @Operation(summary = "Validate current admin token", description = "Returns the authenticated admin identity for route guards and session checks.")
    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "username", principal.getUsername(),
                "role", principal.getRole()
        ));
    }
}
