package com.magiccode.backend.service;

import com.magiccode.backend.dto.LoginRequest;
import com.magiccode.backend.dto.LoginResponse;
import com.magiccode.backend.model.User;
import com.magiccode.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager manager;
    private final JWTService jwtService;

    public LoginResponse login(LoginRequest req) {
        try {
            manager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
        } catch (AuthenticationException ex) {
            log.warn("admin login failed username={}", req.getUsername());
            throw ex;
        }

        User dbUser = userRepository.findByUsername(req.getUsername());

        if (dbUser == null) {
            throw new IllegalStateException("Authenticated user not found");
        }

        String token = jwtService.generateToken(dbUser.getUsername(), dbUser.getRole());
        log.info("admin login succeeded username={} role={}", dbUser.getUsername(), dbUser.getRole());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpiresInSeconds())
                .username(dbUser.getUsername())
                .role(dbUser.getRole())
                .build();
    }
}
