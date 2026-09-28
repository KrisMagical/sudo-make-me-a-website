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
@Schema(description = "Comment response. Visitor comments are PENDING until approved by an admin.")
public class CommentDto {
    @Schema(description = "Comment id.", example = "10")
    private Long id;

    @Schema(description = "Public display name.", example = "Reader")
    private String name;

    @Schema(description = "Comment content.", example = "Thanks for the post.")
    private String content;

    @Schema(description = "Creation time.", example = "2026-06-15T08:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Parent comment id for replies.", example = "9", nullable = true)
    private Long parentId;

    @Schema(description = "Whether this comment was posted by an admin.", example = "false")
    private boolean author;

    @Schema(description = "Moderation status: PENDING means waiting for review, APPROVED is public, REJECTED is hidden.",
            example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
    private CommentStatus status;
}
