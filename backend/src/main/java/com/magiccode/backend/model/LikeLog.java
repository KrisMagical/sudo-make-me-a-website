package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "like_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    @Column(nullable = false)
    private String identifier;
    // true is good, false is not good
    @Column(nullable = false)
    private boolean positive;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
