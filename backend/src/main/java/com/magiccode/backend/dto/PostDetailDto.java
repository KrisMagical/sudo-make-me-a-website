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
public class PostDetailDto {
    private Long id;
    private String title;
    private String content;
    private String slug;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer viewCount;
    private String categoryName;
    private List<CommentDto> comments;
}
