package com.magiccode.backend.controller;

import com.magiccode.backend.config.OpenApiConfig;
import com.magiccode.backend.dto.ImageDto;
import com.magiccode.backend.model.EmbeddedImage;
import com.magiccode.backend.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Media")
public class ImageController {
    private final ImageService imageService;

    @Operation(summary = "Redirect to embedded image", description = "Resolves a stored image record and redirects to its public URL.")
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

    @Operation(summary = "Delete embedded image", description = "Deletes an embedded image record and backing object as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
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
    @Operation(summary = "Upload post image", description = "Uploads an image attached to a post.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/posts/{postId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadPostImage(
            @PathVariable Long postId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadToPost(postId, file));
    }

    @Operation(summary = "List post images", description = "Lists public images attached to a post.")
    @GetMapping("/posts/{postId}/images")
    public ResponseEntity<List<ImageDto>> listPostImages(@PathVariable Long postId) {
        return ResponseEntity.ok(imageService.listPostImages(postId));
    }

    // -------------------- Home Images --------------------
    @Operation(summary = "Upload home image", description = "Uploads an image for the home page.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/home/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadHomeImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadToHome(file));
    }

    @Operation(summary = "List home images", description = "Lists public home page images.")
    @GetMapping("/home/images")
    public ResponseEntity<List<ImageDto>> listHomeImages() {
        return ResponseEntity.ok(imageService.listHomeImages());
    }

    // -------------------- Site Avatar Image --------------------
    @Operation(summary = "Upload site avatar", description = "Uploads the site avatar image.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/site-config/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadSiteAvatarImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadSiteAvatar(file));
    }

    // -------------------- Favicon Image --------------------
    @Operation(summary = "Upload favicon", description = "Uploads the browser favicon.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/browser-icon/favicon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadFaviconImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadFavicon(file));
    }

    // -------------------- Apple Touch Icon Image --------------------
    @Operation(summary = "Upload Apple touch icon", description = "Uploads the Apple touch icon.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/browser-icon/apple-touch-icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadAppleTouchIconImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageService.uploadAppleTouchIcon(file));
    }
}
