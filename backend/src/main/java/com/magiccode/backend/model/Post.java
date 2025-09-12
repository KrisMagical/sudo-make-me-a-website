package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String title;
    @Lob
    @Column(nullable = false,columnDefinition = "MEDIUMTEXT")
    private String content;
    @Column(nullable = false, length = 200, unique = true)
    private String slug;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updateAt;
    @Column(nullable = false)
    @Builder.Default
    private boolean published = true;
    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;
    @Column(nullable = false)
    @Builder.Default
    private Integer dislikeCount = 0;
    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public List<Comment> getComments() {
        return comments == null ? List.of() : List.copyOf(comments);
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments == null ? new ArrayList<>() : new ArrayList<>(comments);
    }
}
