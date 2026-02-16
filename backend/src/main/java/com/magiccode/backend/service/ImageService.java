package com.magiccode.backend.service;

import com.magiccode.backend.dto.ImageDto;
import com.magiccode.backend.mapping.ImageMapper;
import com.magiccode.backend.model.*;
import com.magiccode.backend.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
@Transactional
public class ImageService {
    private final EmbeddedImageRepository embeddedImageRepository;
    private final ImageMapper imageMapper;

    private final PostRepository postRepository;
    private final PageRepository pageRepository;
    private final HomeProfileRepository homeProfileRepository;
    private final SocialRepository socialRepository;
    private final SiteConfigRepository siteConfigRepository;
    private final BrowserIconRepository browserIconRepository;

    // ---------- Upload ----------
    public ImageDto uploadToPost(Long postId, MultipartFile file) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post Not Found"));
        EmbeddedImage saved = save(EmbeddedImage.OwnerType.POST, post.getId(), file);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadToPage(String pageSlug, MultipartFile file) {
        Page page = pageRepository.findBySlug(pageSlug);
        if (page == null) throw new RuntimeException("Page Not Found");
        EmbeddedImage saved = save(EmbeddedImage.OwnerType.PAGE, page.getId(), file);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadToHome(MultipartFile file) {
        HomeProfile home = ensureHomeExists();
        EmbeddedImage saved = save(EmbeddedImage.OwnerType.HOME, home.getId(), file);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadToSocial(Long socialId, MultipartFile file) {
        Social social = socialRepository.findById(socialId)
                .orElseThrow(() -> new RuntimeException("Social Not Found"));
        EmbeddedImage saved = save(EmbeddedImage.OwnerType.SOCIAL, social.getId(), file);
        return imageMapper.toDto(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImageDto uploadSiteAvatar(MultipartFile file) {
        SiteConfig siteConfig = ensureSiteConfigExists();
        EmbeddedImage saved = save(EmbeddedImage.OwnerType.SITE_AVATAR, siteConfig.getId(), file);

        siteConfig.setSiteAvatarImageId(saved.getId());
        siteConfigRepository.save(siteConfig);

        return imageMapper.toDto(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImageDto uploadFavicon(MultipartFile file) {
        BrowserIcon browserIcon = ensureBrowserIconExists();
        EmbeddedImage saved = save(EmbeddedImage.OwnerType.FAVICON, browserIcon.getId(), file);

        browserIcon.setFaviconImageId(saved.getId());
        browserIconRepository.save(browserIcon);

        return imageMapper.toDto(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImageDto uploadAppleTouchIcon(MultipartFile file) {
        BrowserIcon browserIcon = ensureBrowserIconExists();
        EmbeddedImage saved = save(EmbeddedImage.OwnerType.APPLE_TOUCH_ICON, browserIcon.getId(), file);

        browserIcon.setAppleTouchIconImageId(saved.getId());
        browserIconRepository.save(browserIcon);

        return imageMapper.toDto(saved);
    }

    private EmbeddedImage save(EmbeddedImage.OwnerType ownerType, Long ownerId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new RuntimeException("File is Empty");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Unsupported image content-type");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Read file failed");
        }

        EmbeddedImage image = EmbeddedImage.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .originalFilename(generateSafeFilename(file, contentType))
                .contentType(contentType)
                .size(file.getSize())
                .data(bytes)
                .build();

        return embeddedImageRepository.save(image);
    }

    // ---------- List ----------
    @Transactional(readOnly = true)
    public List<ImageDto> listPostImages(Long postId) {
        if (!postRepository.existsById(postId)) throw new RuntimeException("Post Not Found");
        return imageMapper.toDtoList(
                embeddedImageRepository.findAllByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(EmbeddedImage.OwnerType.POST, postId)
        );
    }

    @Transactional(readOnly = true)
    public List<ImageDto> listPageImages(String pageSlug) {
        Page page = pageRepository.findBySlug(pageSlug);
        if (page == null) throw new RuntimeException("Page Not Found");
        return imageMapper.toDtoList(
                embeddedImageRepository.findAllByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(EmbeddedImage.OwnerType.PAGE, page.getId())
        );
    }

    @Transactional(readOnly = true)
    public List<ImageDto> listHomeImages() {
        HomeProfile home = ensureHomeExists();
        return imageMapper.toDtoList(
                embeddedImageRepository.findAllByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(EmbeddedImage.OwnerType.HOME, home.getId())
        );
    }

    // ---------- Get Binary ----------
    @Transactional(readOnly = true)
    public EmbeddedImage get(EmbeddedImage.OwnerType ownerType, Long ownerId, Long imageId) {
        return embeddedImageRepository.findByIdAndOwnerTypeAndOwnerId(imageId, ownerType, ownerId)
                .orElseThrow(() -> new RuntimeException("Image Not Found"));
    }

    // ---------- Delete ----------
    public void delete(EmbeddedImage.OwnerType ownerType, Long ownerId, Long imageId) {
        EmbeddedImage img = get(ownerType, ownerId, imageId);
        embeddedImageRepository.delete(img);
    }

    public void deleteAll(EmbeddedImage.OwnerType ownerType, Long ownerId) {
        embeddedImageRepository.deleteAllByOwnerTypeAndOwnerId(ownerType, ownerId);
    }

    // ---------- Utils ----------
    public MediaType getMediaTypeOrOctet(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private HomeProfile ensureHomeExists() {
        return homeProfileRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> homeProfileRepository.save(
                        HomeProfile.builder().title("Home").content("").build()
                ));
    }

    private String generateSafeFilename(MultipartFile file, String contentType) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && !originalName.trim().isEmpty()) {
            return originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
        String ext = switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            case "image/tiff" -> "tiff";
            default -> null;
        };
        return UUID.randomUUID() + (ext != null ? "." + ext : "");
    }

    private SiteConfig ensureSiteConfigExists() {
        return siteConfigRepository.findByIsActiveTrue()
                .orElseGet(() -> siteConfigRepository.save(
                        SiteConfig.builder()
                                .siteName("我的博客")
                                .authorName("作者")
                                .isActive(true)
                                .build()
                ));
    }

    private BrowserIcon ensureBrowserIconExists() {
        return browserIconRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    BrowserIcon newIcon = BrowserIcon.builder()
                            .isActive(true)
                            .build();
                    BrowserIcon saved = browserIconRepository.save(newIcon);
                    browserIconRepository.flush();
                    return saved;
                });
    }
}
