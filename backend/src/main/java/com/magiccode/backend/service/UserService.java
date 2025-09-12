package com.magiccode.backend.service;

import com.magiccode.backend.model.User;
import com.magiccode.backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Data
public class UserService {
    private UserRepository userRepository;
    private AuthenticationManager manager;
    private JWTService jwtService;

    public String verify(User user) {
        Authentication authentication = manager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(user.getUsername());
        }
        return "Failed";
    }
}
