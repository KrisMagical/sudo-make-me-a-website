package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.magiccode.backend.model.CommentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin comment list/search item.")
public class CommentSearchResultDto {
    @Schema(description = "Comment id.", example = "10")
    private Long id;
    private String name;
    private String email;
    private String content;
    private LocalDateTime createdAt;
    private Long parentId;
    private boolean author;
    private CommentStatus status;

    private Long postId;
    private String postTitle;
    private String postSlug;

    private boolean parentExists;
    private String parentName;
    private String parentContent;

    @Schema(description = "Moderation reason when a comment was flagged or rejected.", example = "too many links", nullable = true)
    private String moderationReason;
}
