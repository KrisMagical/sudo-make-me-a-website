package com.magiccode.backend.controller;

import com.magiccode.backend.model.MaintenanceConfig;
import com.magiccode.backend.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maintenance")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    @GetMapping("/status")
    public ResponseEntity<MaintenanceConfig> getStatus() {
        return ResponseEntity.ok(maintenanceService.getStatus());
    }

    @PutMapping("/update")
    public ResponseEntity<MaintenanceConfig> updateStatus(@RequestBody Map<String, Object> payload) {
        boolean enabled = (boolean) payload.getOrDefault("enabled", false);
        String mode = (String) payload.getOrDefault("mode", "maintenance");
        String username = (String) payload.get("username");
        String password = (String) payload.get("password");
        MaintenanceConfig updated = maintenanceService.updateStatus(enabled, mode, username, password);
        return ResponseEntity.ok(updated);
    }
}
