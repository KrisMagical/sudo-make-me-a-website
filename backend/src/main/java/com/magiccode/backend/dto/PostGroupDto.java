package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostGroupDto {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long coverImageId;
    private String coverImageUrl;
    private List<ImageDto> images;
    private List<PostSummaryDto> posts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
