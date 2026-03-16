package com.magiccode.backend.service;

import com.magiccode.backend.dto.ImageDto;
import com.magiccode.backend.mapping.ImageMapper;
import com.magiccode.backend.model.*;
import com.magiccode.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final EmbeddedImageRepository embeddedImageRepository;
    private final ImageMapper imageMapper;

    private final PostRepository postRepository;
    private final PageRepository pageRepository;
    private final HomeProfileRepository homeProfileRepository;
    private final SocialRepository socialRepository;
    private final SiteConfigRepository siteConfigRepository;
    private final BrowserIconRepository browserIconRepository;

    public static class ProcessedFile {
        public final byte[] data;
        public final String originalFilename;
        public final String contentType;
        public final long size;

        public ProcessedFile(byte[] data, String originalFilename, String contentType, long size) {
            this.data = data;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.size = size;
        }
    }

    public ProcessedFile processFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new RuntimeException("File is Empty");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Unsupported image content-type");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Read file failed", e);
        }

        String safeName = generateSafeFilename(file, contentType);
        return new ProcessedFile(bytes, safeName, contentType, file.getSize());
    }

    @Transactional
    public EmbeddedImage saveImage(EmbeddedImage.OwnerType ownerType, Long ownerId,
                                   byte[] data, String originalFilename, String contentType, long size) {
        EmbeddedImage image = EmbeddedImage.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .originalFilename(originalFilename)
                .contentType(contentType)
                .size(size)
                .data(data)
                .build();
        return embeddedImageRepository.save(image);
    }

    // ---------- Upload ----------
    @Transactional
    protected ImageDto doUploadToPost(Long postId, ProcessedFile pf) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post Not Found"));
        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.POST, post.getId(),
                pf.data, pf.originalFilename, pf.contentType, pf.size);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadToPost(Long postId, MultipartFile file) {
        ProcessedFile pf = processFile(file);
        return doUploadToPost(postId, pf);
    }

    @Transactional
    protected ImageDto doUploadToPage(String pageSlug, ProcessedFile pf) {
        Page page = pageRepository.findBySlug(pageSlug);
        if (page == null) throw new RuntimeException("Page Not Found");
        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.PAGE, page.getId(),
                pf.data, pf.originalFilename, pf.contentType, pf.size);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadToPage(String pageSlug, MultipartFile file) {
        ProcessedFile pf = processFile(file);
        return doUploadToPage(pageSlug, pf);
    }

    @Transactional
    protected ImageDto doUploadToHome(ProcessedFile pf) {
        HomeProfile home = ensureHomeExists();
        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.HOME, home.getId(),
                pf.data, pf.originalFilename, pf.contentType, pf.size);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadToHome(MultipartFile file) {
        ProcessedFile pf = processFile(file);
        return doUploadToHome(pf);
    }

    @Transactional
    protected ImageDto doUploadSiteAvatar(ProcessedFile pf) {
        SiteConfig siteConfig = ensureSiteConfigExists();
        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.SITE_AVATAR, siteConfig.getId(),
                pf.data, pf.originalFilename, pf.contentType, pf.size);
        siteConfig.setSiteAvatarImageId(saved.getId());
        siteConfigRepository.save(siteConfig);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadSiteAvatar(MultipartFile file) {
        ProcessedFile pf = processFile(file);
        return doUploadSiteAvatar(pf);
    }

    @Transactional
    protected ImageDto doUploadFavicon(ProcessedFile pf) {
        BrowserIcon browserIcon = ensureBrowserIconExists();
        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.FAVICON, browserIcon.getId(),
                pf.data, pf.originalFilename, pf.contentType, pf.size);
        browserIcon.setFaviconImageId(saved.getId());
        browserIconRepository.save(browserIcon);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadFavicon(MultipartFile file) {
        ProcessedFile pf = processFile(file);
        return doUploadFavicon(pf);
    }

    @Transactional
    protected ImageDto doUploadAppleTouchIcon(ProcessedFile pf) {
        BrowserIcon browserIcon = ensureBrowserIconExists();
        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.APPLE_TOUCH_ICON, browserIcon.getId(),
                pf.data, pf.originalFilename, pf.contentType, pf.size);
        browserIcon.setAppleTouchIconImageId(saved.getId());
        browserIconRepository.save(browserIcon);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadAppleTouchIcon(MultipartFile file) {
        ProcessedFile pf = processFile(file);
        return doUploadAppleTouchIcon(pf);
    }

    @Transactional
    protected ImageDto doUploadToSocial(Long socialId, ProcessedFile pf) {
        Social social = socialRepository.findById(socialId).orElseThrow(() -> new RuntimeException("Social Not Found"));
        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.SOCIAL, socialId,
                pf.data, pf.originalFilename, pf.contentType, pf.size);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadToSocial(Long socialId, MultipartFile file) {
        ProcessedFile pf = processFile(file);
        return doUploadToSocial(socialId, pf);
    }

    // ---------- List ----------
    @Transactional(readOnly = true)
    public List<ImageDto> listImages(EmbeddedImage.OwnerType ownerType, Long ownerId) {
        return imageMapper.toDtoList(
                embeddedImageRepository.findAllByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(ownerType, ownerId)
        );
    }

    @Transactional(readOnly = true)
    public List<ImageDto> listPostImages(Long postId) {
        if (!postRepository.existsById(postId)) throw new RuntimeException("Post Not Found");
        return listImages(EmbeddedImage.OwnerType.POST, postId);
    }

    @Transactional(readOnly = true)
    public List<ImageDto> listPageImages(String pageSlug) {
        Page page = pageRepository.findBySlug(pageSlug);
        if (page == null) throw new RuntimeException("Page Not Found");
        return listImages(EmbeddedImage.OwnerType.PAGE, page.getId());
    }

    @Transactional(readOnly = true)
    public List<ImageDto> listHomeImages() {
        HomeProfile home = homeProfileRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("Home not configured"));
        return listImages(EmbeddedImage.OwnerType.HOME, home.getId());
    }

    // ---------- Get Binary ----------
    @Transactional(readOnly = true)
    public EmbeddedImage get(EmbeddedImage.OwnerType ownerType, Long ownerId, Long imageId) {
        return embeddedImageRepository.findByIdAndOwnerTypeAndOwnerId(imageId, ownerType, ownerId)
                .orElseThrow(() -> new RuntimeException("Image Not Found"));
    }

    // ---------- Delete ----------
    @Transactional
    public void delete(EmbeddedImage.OwnerType ownerType, Long ownerId, Long imageId) {
        EmbeddedImage img = get(ownerType, ownerId, imageId);
        embeddedImageRepository.delete(img);
    }

    @Transactional
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

    @Transactional
    protected HomeProfile ensureHomeExists() {
        return homeProfileRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> homeProfileRepository.save(
                        HomeProfile.builder().title("Home").content("").build()
                ));
    }

    @Transactional
    protected SiteConfig ensureSiteConfigExists() {
        return siteConfigRepository.findByIsActiveTrue()
                .orElseGet(() -> siteConfigRepository.save(
                        SiteConfig.builder()
                                .siteName("我的博客")
                                .authorName("作者")
                                .isActive(true)
                                .build()
                ));
    }

    @Transactional
    protected BrowserIcon ensureBrowserIconExists() {
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
}