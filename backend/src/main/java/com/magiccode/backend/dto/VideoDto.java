package com.magiccode.backend.dto;

import com.magiccode.backend.model.EmbeddedVideo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoDto {
    private Long id;
    private EmbeddedVideo.OwnerType ownerType;
    private Long ownerId;

    private String provider;
    private String sourceUrl;
    private String embedUrl;
    private String title;

    private Integer orderIndex;
    private LocalDateTime createdAt;
}
