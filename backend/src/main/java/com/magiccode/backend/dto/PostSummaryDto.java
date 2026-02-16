package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostSummaryDto {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer viewCount;
    private String categoryName;
}
