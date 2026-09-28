package com.magiccode.backend.dto;

import com.magiccode.backend.model.EmbeddedImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Uploaded or embedded image metadata.")
public class ImageDto {
    @Schema(description = "Image id.", example = "21")
    private Long id;
    @Schema(description = "Entity type that owns the image.", example = "POST")
    private EmbeddedImage.OwnerType ownerType;
    @Schema(description = "Owner entity id.", example = "1")
    private Long ownerId;
    @Schema(description = "Original uploaded filename.", example = "cover.png")
    private String originalFilename;
    @Schema(description = "MIME content type.", example = "image/png")
    private String contentType;
    @Schema(description = "File size in bytes.", example = "124000")
    private Long size;
    @Schema(description = "Upload time.", example = "2026-06-15T08:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "Public image URL.", example = "https://cdn.example.test/images/cover.png")
    private String url;
}
