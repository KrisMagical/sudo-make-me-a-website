package com.magiccode.backend.service;

import com.magiccode.backend.dto.BrowserIconDto;
import com.magiccode.backend.dto.CategoryDto;
import com.magiccode.backend.dto.SidebarDto;
import com.magiccode.backend.dto.SiteConfigDto;
import com.magiccode.backend.mapping.BrowserIconMapper;
import com.magiccode.backend.mapping.CategoryMapper;
import com.magiccode.backend.mapping.SidebarMapper;
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
    private final SidebarMapper sidebarMapper;
    private final SiteConfigRepository siteConfigRepository;
    private final PageRepository pageRepository;
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

        List<Page> allPages = pageRepository.findAll();

        List<Category> categories = categoryRepository.findAll();

        BrowserIcon browserIcon = browserIconRepository.findByIsActiveTrue()
                .orElseGet(() -> BrowserIcon.builder()
                        .build());

        SiteConfigDto siteConfigDto = buildSiteConfigDto(siteConfig);

        BrowserIconDto browserIconDto = buildBrowserIconDto(browserIcon);

        List<CategoryDto> categoryDtos = categories.stream()
                .map(categoryMapper::toCategoryDto)
                .toList();

        return SidebarDto.builder()
                .siteConfig(siteConfigDto)
                .pages(sidebarMapper.buildPageTree(allPages))
                .categories(categoryDtos)
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
        Long oldSiteConfigId = siteConfigRepository.findByIsActiveTrue()
                .map(SiteConfig::getId).orElse(null);

        siteConfigRepository.findAll().forEach(config -> config.setIsActive(false));
        siteConfigRepository.flush();

        SiteConfig siteConfig = SiteConfig.builder()
                .siteName(dto.getSiteName())
                .authorName(dto.getAuthorName())
                .siteAvatarImageId(dto.getSiteAvatarImageId())
                .footerText(dto.getFooterText())
                .metaDescription(dto.getMetaDescription())
                .metaKeywords(dto.getMetaKeywords())
                .copyrightText(dto.getCopyrightText())
                .isActive(true)
                .build();
        SiteConfig saved = siteConfigRepository.save(siteConfig);

        if (dto.getSiteAvatarImageId() != null && oldSiteConfigId != null) {
            EmbeddedImage transferred = transferImageOwnership(
                    EmbeddedImage.OwnerType.SITE_AVATAR,
                    oldSiteConfigId,
                    saved.getId(),
                    dto.getSiteAvatarImageId()
            );
            if (transferred == null) {
                saved.setSiteAvatarImageId(null);
                siteConfigRepository.save(saved);
            }
        }

        return buildSiteConfigDto(saved);
    }

    public BrowserIconDto getBrowserIcon() {
        BrowserIcon browserIcon = browserIconRepository.findByIsActiveTrue()
                .orElseGet(() -> BrowserIcon.builder().build());

        return buildBrowserIconDto(browserIcon);
    }

    public BrowserIconDto updateBrowserIcon(BrowserIconDto dto) {
        Long oldBrowserIconId = browserIconRepository.findByIsActiveTrue()
                .map(BrowserIcon::getId).orElse(null);

        browserIconRepository.findAll().forEach(icon -> icon.setIsActive(false));
        browserIconRepository.flush();

        BrowserIcon browserIcon = BrowserIcon.builder()
                .faviconImageId(dto.getFaviconImageId())
                .appleTouchIconImageId(dto.getAppleTouchIconImageId())
                .isActive(true)
                .build();
        BrowserIcon saved = browserIconRepository.save(browserIcon);

        if (dto.getFaviconImageId() != null && oldBrowserIconId != null) {
            EmbeddedImage transferred = transferImageOwnership(
                    EmbeddedImage.OwnerType.FAVICON,
                    oldBrowserIconId,
                    saved.getId(),
                    dto.getFaviconImageId()
            );
            if (transferred == null) {
                saved.setFaviconImageId(null);
            }
        }

        if (dto.getAppleTouchIconImageId() != null && oldBrowserIconId != null) {
            EmbeddedImage transferred = transferImageOwnership(
                    EmbeddedImage.OwnerType.APPLE_TOUCH_ICON,
                    oldBrowserIconId,
                    saved.getId(),
                    dto.getAppleTouchIconImageId()
            );
            if (transferred == null) {
                saved.setAppleTouchIconImageId(null);
            }
        }

        if (saved.getFaviconImageId() == null && saved.getAppleTouchIconImageId() == null) {
        }
        browserIconRepository.save(saved);

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
