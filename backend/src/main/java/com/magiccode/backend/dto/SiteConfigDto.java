package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteConfigDto {
    private Long id;
    private String siteName;
    private String authorName;
    private Long siteAvatarImageId;
    private String siteAvatarUrl;
    private String footerText;
    private String metaDescription;
    private String metaKeywords;
    private String copyrightText;
    private Boolean isActive;
}
