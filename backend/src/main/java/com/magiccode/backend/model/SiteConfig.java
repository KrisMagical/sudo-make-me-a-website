package com.magiccode.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "site_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", nullable = false, length = 100)
    private String siteName;

    @Column(name = "author_name", length = 100)
    private String authorName;

    @Column(name = "site_avatar_image_id")
    private Long siteAvatarImageId;

    @Column(name = "footer_text", columnDefinition = "TEXT")
    private String footerText;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "meta_keywords", columnDefinition = "TEXT")
    private String metaKeywords;

    @Column(name = "copyright_text", columnDefinition = "TEXT")
    private String copyrightText;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
