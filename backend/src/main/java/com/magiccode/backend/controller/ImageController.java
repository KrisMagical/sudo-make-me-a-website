package com.magiccode.backend.controller;

import com.magiccode.backend.dto.ImageDto;
import com.magiccode.backend.model.EmbeddedImage;
import com.magiccode.backend.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ImageController {
    private final ImageService imageService;

    @GetMapping("/images/{ownerType}/{ownerId}/{imageId}")
    public ResponseEntity<Void> getEmbeddedImage(
            @PathVariable EmbeddedImage.OwnerType ownerType,
            @PathVariable Long ownerId,
            @PathVariable Long imageId) {

        EmbeddedImage image = imageService.get(ownerType, ownerId, imageId);
        return ResponseEntity.status(302)
                .location(URI.create(image.getUrl()))
                .build();
    }

    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/images/{ownerType}/{ownerId}/{imageId}")
    public ResponseEntity<Void> deleteEmbeddedImage(
            @PathVariable EmbeddedImage.OwnerType ownerType,
            @PathVariable Long ownerId,
            @PathVariable Long imageId) {
        imageService.delete(ownerType, ownerId, imageId);
        return ResponseEntity.noContent().build();
    }

    // -------------------- Post Images --------------------
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/posts/{postId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadPostImage(
            @PathVariable Long postId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadToPost(postId, file));
    }

    @GetMapping("/posts/{postId}/images")
    public ResponseEntity<List<ImageDto>> listPostImages(@PathVariable Long postId) {
        return ResponseEntity.ok(imageService.listPostImages(postId));
    }

    // -------------------- Home Images --------------------
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/home/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadHomeImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadToHome(file));
    }

    @GetMapping("/home/images")
    public ResponseEntity<List<ImageDto>> listHomeImages() {
        return ResponseEntity.ok(imageService.listHomeImages());
    }

    // -------------------- Site Avatar Image --------------------
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/site-config/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadSiteAvatarImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadSiteAvatar(file));
    }

    // -------------------- Favicon Image --------------------
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/browser-icon/favicon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadFaviconImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadFavicon(file));
    }

    // -------------------- Apple Touch Icon Image --------------------
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/browser-icon/apple-touch-icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadAppleTouchIconImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadAppleTouchIcon(file));
    }
}
