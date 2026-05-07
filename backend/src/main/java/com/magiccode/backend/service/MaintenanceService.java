package com.magiccode.backend.service;

import com.magiccode.backend.model.MaintenanceConfig;
import com.magiccode.backend.model.User;
import com.magiccode.backend.repository.MaintenanceConfigRepository;
import com.magiccode.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MaintenanceService {
    private final MaintenanceConfigRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            MaintenanceConfig defaultConfig = MaintenanceConfig.builder()
                    .enabled(false)
                    .mode("maintenance")
                    .updatedAt(LocalDateTime.now())
                    .build();
            repository.save(defaultConfig);
        }
    }

    public MaintenanceConfig getStatus() {
        return repository.findAll().stream().findFirst().orElse(null);
    }

    public MaintenanceConfig updateStatus(boolean enabled, String mode, String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Invalid credentials");
        }
        if (!"ROOT".equals(user.getRole())) {
            throw new RuntimeException("Insufficient privileges");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        MaintenanceConfig config = getStatus();
        config.setEnabled(enabled);
        config.setMode(mode);
        config.setUpdatedAt(LocalDateTime.now());
        return repository.save(config);
    }
}
