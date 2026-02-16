package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pages", indexes = {
        @Index(name = "idx_pages_slug", columnList = "slug"),
        @Index(name = "index_pages_parent", columnList = "parent_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200, unique = true)
    private String slug;
    @Column(nullable = false, length = 200)
    private String title;
    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Page parent;
    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;
}
