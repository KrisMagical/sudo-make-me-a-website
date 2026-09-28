package com.magiccode.backend.config;

import com.magiccode.backend.model.User;
import com.magiccode.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitialAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${blog.admin.username:}")
    private String adminUsername;

    @Value("${blog.admin.password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        String username = adminUsername.trim();
        if (userRepository.findByUsername(username) != null) {
            return;
        }
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(adminPassword))
                .role("ROOT")
                .build();
        userRepository.save(user);
    }
}
