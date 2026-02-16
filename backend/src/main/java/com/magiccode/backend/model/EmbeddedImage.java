package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "embedded_images",
        indexes = {
                @Index(name = "idx_embedded_images_owner", columnList = "owner_type, owner_id"),
                @Index(name = "idx_embedded_images_owner_created", columnList = "owner_type, owner_id, created_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddedImage {
    public enum OwnerType {POST, PAGE, HOME, SOCIAL, SITE_AVATAR, FAVICON, APPLE_TOUCH_ICON}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private OwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
