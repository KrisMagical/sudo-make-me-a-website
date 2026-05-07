package com.magiccode.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class PostSummaryDto {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer viewCount;
    private String categoryName;
    private List<String> collectionNames;
}
