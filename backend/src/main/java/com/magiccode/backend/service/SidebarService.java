package com.magiccode.backend.service;

import com.magiccode.backend.dto.BrowserIconDto;
import com.magiccode.backend.dto.SidebarDto;
import com.magiccode.backend.dto.SiteConfigDto;
import com.magiccode.backend.mapping.BrowserIconMapper;
import com.magiccode.backend.mapping.CategoryMapper;
import com.magiccode.backend.mapping.SiteConfigMapper;
import com.magiccode.backend.model.*;
import com.magiccode.backend.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Data
@Transactional
public class SidebarService {
    private final SiteConfigRepository siteConfigRepository;
    private final CategoryRepository categoryRepository;
    private final BrowserIconRepository browserIconRepository;
    private final SiteConfigMapper siteConfigMapper;
    private final BrowserIconMapper browserIconMapper;
    private final CategoryMapper categoryMapper;
    private final EmbeddedImageRepository embeddedImageRepository;

    public SidebarDto getSidebarData() {
        SiteConfig siteConfig = siteConfigRepository.findByIsActiveTrue()
                .orElseGet(() -> SiteConfig.builder()
                        .siteName("我的博客")
                        .authorName("作者")
                        .build());

        List<Category> categories = categoryRepository.findAll();

        BrowserIcon browserIcon = browserIconRepository.findByIsActiveTrue()
                .orElseGet(() -> BrowserIcon.builder()
                        .build());

        SiteConfigDto siteConfigDto = buildSiteConfigDto(siteConfig);

        BrowserIconDto browserIconDto = buildBrowserIconDto(browserIcon);

        return SidebarDto.builder()
                .siteConfig(siteConfigDto)
                .categories(categories.stream().map(categoryMapper::toCategoryDto).toList())
                .browserIcon(browserIconDto)
                .build();
    }

    public SiteConfigDto getSiteConfig() {
        SiteConfig siteConfig = siteConfigRepository.findByIsActiveTrue()
                .orElseGet(() -> SiteConfig.builder()
                        .siteName("My Blog")
                        .authorName("KrisMagic")
                        .build());

        return buildSiteConfigDto(siteConfig);
    }

    public SiteConfigDto updateSiteConfig(SiteConfigDto dto) {
        SiteConfig siteConfig = siteConfigRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    SiteConfig newConfig = SiteConfig.builder()
                            .isActive(true)
                            .build();
                    return newConfig;
                });

        siteConfig.setSiteName(dto.getSiteName());
        siteConfig.setAuthorName(dto.getAuthorName());
        siteConfig.setFooterText(dto.getFooterText());
        siteConfig.setMetaDescription(dto.getMetaDescription());
        siteConfig.setMetaKeywords(dto.getMetaKeywords());
        siteConfig.setCopyrightText(dto.getCopyrightText());

        if (dto.getSiteAvatarImageId() != null) {
            Long oldOwnerId = siteConfigRepository.findByIsActiveTrue()
                    .map(SiteConfig::getId)
                    .orElse(null);
            if (oldOwnerId != null && !oldOwnerId.equals(siteConfig.getId())) {
                EmbeddedImage transferred = transferImageOwnership(
                        EmbeddedImage.OwnerType.SITE_AVATAR,
                        oldOwnerId,
                        siteConfig.getId(),
                        dto.getSiteAvatarImageId()
                );
                if (transferred == null) {
                    siteConfig.setSiteAvatarImageId(null);
                } else {
                    siteConfig.setSiteAvatarImageId(dto.getSiteAvatarImageId());
                }
            } else {
                siteConfig.setSiteAvatarImageId(dto.getSiteAvatarImageId());
            }
        } else {
            siteConfig.setSiteAvatarImageId(null);
        }

        siteConfig.setIsActive(true);
        SiteConfig saved = siteConfigRepository.save(siteConfig);

        return buildSiteConfigDto(saved);
    }

    public BrowserIconDto getBrowserIcon() {
        BrowserIcon browserIcon = browserIconRepository.findByIsActiveTrue()
                .orElseGet(() -> BrowserIcon.builder().build());

        return buildBrowserIconDto(browserIcon);
    }

    public BrowserIconDto updateBrowserIcon(BrowserIconDto dto) {
        BrowserIcon browserIcon = browserIconRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    BrowserIcon newIcon = BrowserIcon.builder()
                            .isActive(true)
                            .build();
                    return newIcon;
                });

        browserIcon.setFaviconImageId(dto.getFaviconImageId());
        browserIcon.setAppleTouchIconImageId(dto.getAppleTouchIconImageId());

        Long oldOwnerId = browserIconRepository.findByIsActiveTrue()
                .map(BrowserIcon::getId)
                .orElse(null);
        if (oldOwnerId != null && !oldOwnerId.equals(browserIcon.getId())) {
            if (dto.getFaviconImageId() != null) {
                EmbeddedImage transferredFav = transferImageOwnership(
                        EmbeddedImage.OwnerType.FAVICON,
                        oldOwnerId,
                        browserIcon.getId(),
                        dto.getFaviconImageId()
                );
                if (transferredFav == null) {
                    browserIcon.setFaviconImageId(null);
                }
            }
            if (dto.getAppleTouchIconImageId() != null) {
                EmbeddedImage transferredApple = transferImageOwnership(
                        EmbeddedImage.OwnerType.APPLE_TOUCH_ICON,
                        oldOwnerId,
                        browserIcon.getId(),
                        dto.getAppleTouchIconImageId()
                );
                if (transferredApple == null) {
                    browserIcon.setAppleTouchIconImageId(null);
                }
            }
        } else {
            browserIcon.setFaviconImageId(dto.getFaviconImageId());
            browserIcon.setAppleTouchIconImageId(dto.getAppleTouchIconImageId());
        }

        browserIcon.setIsActive(true);
        BrowserIcon saved = browserIconRepository.save(browserIcon);

        return buildBrowserIconDto(saved);
    }


    private SiteConfigDto buildSiteConfigDto(SiteConfig siteConfig) {
        String siteAvatarUrl = null;
        if (siteConfig.getSiteAvatarImageId() != null) {
            siteAvatarUrl = getImageUrl(EmbeddedImage.OwnerType.SITE_AVATAR,
                    siteConfig.getId(), siteConfig.getSiteAvatarImageId());
        }

        return SiteConfigDto.builder()
                .id(siteConfig.getId())
                .siteName(siteConfig.getSiteName())
                .authorName(siteConfig.getAuthorName())
                .siteAvatarImageId(siteConfig.getSiteAvatarImageId())
                .siteAvatarUrl(siteAvatarUrl)
                .footerText(siteConfig.getFooterText())
                .metaDescription(siteConfig.getMetaDescription())
                .metaKeywords(siteConfig.getMetaKeywords())
                .copyrightText(siteConfig.getCopyrightText())
                .isActive(siteConfig.getIsActive())
                .build();
    }

    private BrowserIconDto buildBrowserIconDto(BrowserIcon browserIcon) {
        String faviconUrl = null;
        String appleTouchIconUrl = null;

        if (browserIcon.getId() != null) {
            if (browserIcon.getFaviconImageId() != null) {
                faviconUrl = getImageUrl(EmbeddedImage.OwnerType.FAVICON,
                        browserIcon.getId(), browserIcon.getFaviconImageId());
            }

            if (browserIcon.getAppleTouchIconImageId() != null) {
                appleTouchIconUrl = getImageUrl(EmbeddedImage.OwnerType.APPLE_TOUCH_ICON,
                        browserIcon.getId(), browserIcon.getAppleTouchIconImageId());
            }
        }

        return BrowserIconDto.builder()
                .id(browserIcon.getId())
                .faviconImageId(browserIcon.getFaviconImageId())
                .faviconUrl(faviconUrl)
                .appleTouchIconImageId(browserIcon.getAppleTouchIconImageId())
                .appleTouchIconUrl(appleTouchIconUrl)
                .isActive(browserIcon.getIsActive())
                .build();
    }

    // 获取图片URL
    private String getImageUrl(EmbeddedImage.OwnerType ownerType, Long ownerId, Long imageId) {
        return "/api/images/" + ownerType + "/" + ownerId + "/" + imageId;
    }

    private EmbeddedImage transferImageOwnership(EmbeddedImage.OwnerType ownerType, Long oldOwnerId, Long newOwnerId, Long imageId) {
        if (imageId == null) return null;
        return embeddedImageRepository.findById(imageId)
                .map(image -> {
                    if (image.getOwnerType() == ownerType && image.getOwnerId().equals(oldOwnerId)) {
                        image.setOwnerId(newOwnerId);
                        return embeddedImageRepository.save(image);
                    }
                    return null;
                })
                .orElse(null);
    }
}
