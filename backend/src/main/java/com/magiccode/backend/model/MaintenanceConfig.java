package com.magiccode.backend.model;

import jakarta.persistence.*;
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
public class MaintenanceConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean enabled;
    private String mode; // "updating" 或 "maintenance"
    private LocalDateTime updatedAt;
}
