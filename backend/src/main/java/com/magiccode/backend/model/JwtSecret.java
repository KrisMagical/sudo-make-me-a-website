package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "jwt_secret")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtSecret {

    @Id
    private Long id;

    @Column(name = "secret_base64", nullable = false, length = 512)
    private String secretBase64;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

