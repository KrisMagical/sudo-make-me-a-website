package com.magiccode.backend.controller;

import com.magiccode.backend.dto.LoginRequest;
import com.magiccode.backend.dto.LoginResponse;
import com.magiccode.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(userService.login(req));
    }
}
