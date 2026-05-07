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
public class PostDetailDto {
    private Long id;
    private String title;
    private String content;
    private String slug;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer viewCount;
    private String categoryName;
    private List<CommentDto> comments;
    private List<ImageDto> images;
    private List<VideoDto> videos;
    private List<String> collectionNames;
}
