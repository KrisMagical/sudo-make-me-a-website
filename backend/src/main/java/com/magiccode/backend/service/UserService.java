package com.magiccode.backend.service;

import com.magiccode.backend.dto.LoginRequest;
import com.magiccode.backend.dto.LoginResponse;
import com.magiccode.backend.model.User;
import com.magiccode.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager manager;
    private final JWTService jwtService;

    public LoginResponse login(LoginRequest req) {
        manager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        User dbUser = userRepository.findByUsername(req.getUsername());

        if (dbUser == null) {
            throw new IllegalStateException("认证通过但用户不存在，检查数据一致性");
        }

        String token = jwtService.generateToken(dbUser.getUsername(), dbUser.getRole());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpiresInSeconds())
                .username(dbUser.getUsername())
                .role(dbUser.getRole())
                .build();
    }
}
