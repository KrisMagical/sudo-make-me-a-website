package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_group_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_group_id", "post_id"}),
        indexes = {
                @Index(name = "idx_post_group_items_group", columnList = "post_group_id"),
                @Index(name = "idx_post_group_items_post", columnList = "post_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostGroupItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_group_id", nullable = false)
    private PostGroup postGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @CreationTimestamp
    @Column(name = "added_at")
    private LocalDateTime addedAt;
}
