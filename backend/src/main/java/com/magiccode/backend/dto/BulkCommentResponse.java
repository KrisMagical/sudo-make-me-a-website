package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bulk comment moderation response.")
public class BulkCommentResponse {
    @Schema(description = "Applied action.", example = "APPROVE")
    private BulkCommentAction action;

    @Schema(description = "Number of affected comments.", example = "3")
    private int affected;
}
