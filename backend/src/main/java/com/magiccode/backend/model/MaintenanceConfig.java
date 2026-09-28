package com.magiccode.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Maintenance mode configuration.")
public class MaintenanceConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Config id.", example = "1")
    private Long id;

    @Schema(description = "Whether maintenance mode is enabled.", example = "false")
    private Boolean enabled;

    @Schema(description = "Maintenance display mode.", example = "maintenance", allowableValues = {"updating", "maintenance"})
    private String mode;

    @Schema(description = "Last update time.", example = "2026-06-15T08:30:00")
    private LocalDateTime updatedAt;
}
