package com.magiccode.backend.controller;

import com.magiccode.backend.config.OpenApiConfig;
import com.magiccode.backend.model.MaintenanceConfig;
import com.magiccode.backend.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maintenance")
@Tag(name = "Admin Maintenance")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    @Operation(summary = "Get maintenance status", description = "Returns the current public maintenance mode state.")
    @GetMapping("/status")
    public ResponseEntity<MaintenanceConfig> getStatus() {
        return ResponseEntity.ok(maintenanceService.getStatus());
    }

    @Operation(summary = "Update maintenance status", description = "Updates maintenance mode. Authentication is required by the global security policy.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
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
