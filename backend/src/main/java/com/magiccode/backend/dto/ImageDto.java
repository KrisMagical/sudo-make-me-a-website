package com.magiccode.backend.dto;

import com.magiccode.backend.model.EmbeddedImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageDto {
    private Long id;
    private EmbeddedImage.OwnerType ownerType;
    private Long ownerId;
    private String originalFilename;
    private String contentType;
    private Long size;
    private LocalDateTime createdAt;
    private String url;
}
