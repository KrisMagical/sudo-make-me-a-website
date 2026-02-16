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
        name = "embedded_videos",
        indexes = {
                @Index(name = "idx_embedded_videos_owner", columnList = "owner_type, owner_id"),
                @Index(name = "idx_embedded_videos_owner_order", columnList = "owner_type, owner_id, order_index")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddedVideo {
    public enum OwnerType {
        POST, PAGE, HOME
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private OwnerType ownerType;
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false,length = 3000)
    private String sourceUrl;

    @Column(nullable = false, length = 3000)
    private String embedUrl;

    @Column(nullable = true, length = 2000)
    private String title;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
