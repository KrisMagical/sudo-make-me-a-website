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
@Schema(description = "Admin comment moderation statistics.")
public class CommentStatsDto {
    @Schema(description = "Pending comments.", example = "3")
    private long pending;

    @Schema(description = "Approved comments.", example = "20")
    private long approved;

    @Schema(description = "Rejected comments.", example = "5")
    private long rejected;

    @Schema(description = "Total comments.", example = "28")
    private long total;
}
