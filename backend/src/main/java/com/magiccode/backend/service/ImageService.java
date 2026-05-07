package com.magiccode.backend.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.magiccode.backend.dto.ImageDto;
import com.magiccode.backend.mapping.ImageMapper;
import com.magiccode.backend.model.*;
import com.magiccode.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final EmbeddedImageRepository embeddedImageRepository;
    private final ImageMapper imageMapper;
    private final PostRepository postRepository;
    private final HomeProfileRepository homeProfileRepository;
    private final SocialRepository socialRepository;
    private final SiteConfigRepository siteConfigRepository;
    private final BrowserIconRepository browserIconRepository;
    private final PostGroupRepository postGroupRepository;


    private final TransactionTemplate transactionTemplate;

    private final OSS ossClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;
    @Value("${aliyun.oss.cdn-domain}")
    private String cdnDomain;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private UploadResult uploadToOSS(MultipartFile file, EmbeddedImage.OwnerType ownerType, Long ownerId) {
        if (file == null || file.isEmpty()) throw new RuntimeException("File is Empty");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Unsupported image content-type");
        }

        String originalFilename = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String safeName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String dataPath = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = getFileExtension(originalFilename);

        String objectKey = String.format("%s/%d/%s/%s_%s%s",
                ownerType.name().toLowerCase(),
                ownerId,
                dataPath,
                uuid,
                safeName,
                extension != null ? "." + extension : "");

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, objectKey, inputStream, metadata);
            ossClient.putObject(putRequest);
        } catch (IOException e) {
            throw new RuntimeException("Upload to OSS failed", e);
        }

        String normalizedCdn = normalizeCdnUrl(cdnDomain);
        String url = normalizedCdn.endsWith("/") ? normalizedCdn + objectKey : normalizedCdn + "/" + objectKey;
        return new UploadResult(objectKey, url);
    }

    private void deleteFromOSS(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return;
        }
        ossClient.deleteObject(bucketName, objectKey);
    }

    public EmbeddedImage saveImage(EmbeddedImage.OwnerType ownerType, Long ownerId,
                                   String originalFilename, String contentType, long size,
                                   String objectKey, String url) {
        EmbeddedImage image = EmbeddedImage.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .originalFilename(originalFilename)
                .contentType(contentType)
                .size(size)
                .objectKey(objectKey)
                .url(url)
                .build();
        return embeddedImageRepository.save(image);
    }

    @Transactional
    public ImageDto uploadToPost(Long postId, MultipartFile file) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post Not Found"));

        UploadResult upload = uploadToOSS(file, EmbeddedImage.OwnerType.POST, post.getId());

        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.POST, post.getId(),
                file.getOriginalFilename(), file.getContentType(), file.getSize(),
                upload.objectKey, upload.url);

        return imageMapper.toDto(saved);
    }


    @Transactional
    public ImageDto uploadToPostGroup(Long postGroupId, MultipartFile file) {
        PostGroup postGroup = postGroupRepository.findById(postGroupId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        UploadResult upload = uploadToOSS(file, EmbeddedImage.OwnerType.COLLECTION, postGroup.getId());
        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.COLLECTION, postGroup.getId(),
                file.getOriginalFilename(), file.getContentType(), file.getSize(),
                upload.objectKey, upload.url);
        return imageMapper.toDto(saved);
    }

    public ImageDto uploadSiteAvatar(MultipartFile file) {
        SiteConfig siteConfig = ensureSiteConfigExists();

        UploadResult upload = uploadToOSS(file, EmbeddedImage.OwnerType.SITE_AVATAR, siteConfig.getId());

        EmbeddedImage saved = transactionTemplate.execute(status -> {
            EmbeddedImage image = saveImage(EmbeddedImage.OwnerType.SITE_AVATAR, siteConfig.getId(),
                    file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    upload.objectKey, upload.url);

            siteConfig.setSiteAvatarImageId(image.getId());
            siteConfigRepository.save(siteConfig);
            return image;
        });

        return imageMapper.toDto(saved);
    }

    public ImageDto uploadFavicon(MultipartFile file) {
        BrowserIcon browserIcon = ensureBrowserIconExists();

        UploadResult upload = uploadToOSS(file, EmbeddedImage.OwnerType.FAVICON, browserIcon.getId());

        EmbeddedImage saved = transactionTemplate.execute(status -> {
            EmbeddedImage image = saveImage(EmbeddedImage.OwnerType.FAVICON, browserIcon.getId(),
                    file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    upload.objectKey, upload.url);

            browserIcon.setFaviconImageId(image.getId());
            browserIconRepository.save(browserIcon);
            return image;
        });

        return imageMapper.toDto(saved);
    }

    public ImageDto uploadAppleTouchIcon(MultipartFile file) {
        BrowserIcon browserIcon = ensureBrowserIconExists();

        UploadResult upload = uploadToOSS(file, EmbeddedImage.OwnerType.APPLE_TOUCH_ICON, browserIcon.getId());

        EmbeddedImage saved = transactionTemplate.execute(status -> {
            EmbeddedImage image = saveImage(EmbeddedImage.OwnerType.APPLE_TOUCH_ICON, browserIcon.getId(),
                    file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    upload.objectKey, upload.url);

            browserIcon.setAppleTouchIconImageId(image.getId());
            browserIconRepository.save(browserIcon);
            return image;
        });

        return imageMapper.toDto(saved);
    }

    public ImageDto uploadToSocial(Long socialId, MultipartFile file) {
        Social social = socialRepository.findById(socialId)
                .orElseThrow(() -> new RuntimeException("Social Not Found"));

        UploadResult upload = uploadToOSS(file, EmbeddedImage.OwnerType.SOCIAL, social.getId());

        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.SOCIAL, social.getId(),
                file.getOriginalFilename(), file.getContentType(), file.getSize(),
                upload.objectKey, upload.url);

        return imageMapper.toDto(saved);
    }

    public ImageDto uploadToHome(MultipartFile file) {
        HomeProfile home = ensureHomeExists();

        UploadResult upload = uploadToOSS(file, EmbeddedImage.OwnerType.HOME, home.getId());
        EmbeddedImage saved = saveImage(EmbeddedImage.OwnerType.HOME, home.getId(),
                file.getOriginalFilename(), file.getContentType(), file.getSize(),
                upload.objectKey, upload.url);

        return imageMapper.toDto(saved);
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
    public List<ImageDto> listHomeImages() {
        HomeProfile home = homeProfileRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("Home not configured"));
        return listImages(EmbeddedImage.OwnerType.HOME, home.getId());
    }

    @Transactional(readOnly = true)
    public EmbeddedImage get(EmbeddedImage.OwnerType ownerType, Long ownerId, Long imageId) {
        return embeddedImageRepository.findByIdAndOwnerTypeAndOwnerId(imageId, ownerType, ownerId)
                .orElseThrow(() -> new RuntimeException("Image Not Found"));
    }

    @Transactional
    public void delete(EmbeddedImage.OwnerType ownerType, Long ownerId, Long imageId) {
        EmbeddedImage img = get(ownerType, ownerId, imageId);
        deleteFromOSS(img.getObjectKey());
        embeddedImageRepository.delete(img);
    }

    @Transactional
    public void deleteAll(EmbeddedImage.OwnerType ownerType, Long ownerId) {
        List<EmbeddedImage> images = embeddedImageRepository.findAllByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(ownerType, ownerId);
        for (EmbeddedImage img : images) {
            deleteFromOSS(img.getObjectKey());
        }
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
                        HomeProfile.builder()
                                .title("Home")
                                .content("")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
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

    private static String generateSafeFile(MultipartFile file, String contentType) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && !originalName.trim().isEmpty()) {
            return originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
        String ext = getFileExtension(null);
        return UUID.randomUUID() + (ext != null ? "." + ext : "");
    }

    private static String getFileExtension(String filename) {
        if (filename == null) return null;
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private String normalizeCdnUrl(String cdnDomain) {
        if (cdnDomain == null || cdnDomain.trim().isEmpty()) {
            return "";
        }
        if (!cdnDomain.startsWith("http://") && !cdnDomain.startsWith("https://")) {
            return "https://" + cdnDomain;
        }
        return cdnDomain;
    }

    private static class UploadResult {
        String objectKey;
        String url;

        UploadResult(String objectKey, String url) {
            this.objectKey = objectKey;
            this.url = url;
        }
    }
}