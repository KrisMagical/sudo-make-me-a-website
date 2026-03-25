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
public class CommentSearchResultDto {
    private Long id;
    private String name;
    private String email;
    private String content;
    private LocalDateTime createdAt;
    private Long parentId;
    private boolean author;

    private Long postId;
    private String postTitle;
    private String postSlug;

    private boolean parentExists;
    private String parentName;
    private String parentContent;
}
