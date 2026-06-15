package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bulk comment moderation request.")
public class BulkCommentRequest {
    @NotEmpty(message = "commentIds must not be empty")
    @Schema(description = "Comment ids to update.", example = "[1,2,3]")
    private List<Long> commentIds;

    @NotNull(message = "action is required")
    @Schema(description = "Bulk action.", example = "APPROVE")
    private BulkCommentAction action;
}
