package com.magiccode.backend.controller;

import com.magiccode.backend.model.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "username", principal.getUsername(),
                "role", principal.getRole()
        ));
    }
}
