package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paged admin comment list response.")
public class AdminCommentPageResponse {
    @Schema(description = "Comments in the current page.")
    private List<CommentSearchResultDto> items;

    @Schema(description = "Current zero-based page.", example = "0")
    private int page;

    @Schema(description = "Page size.", example = "20")
    private int size;

    @Schema(description = "Total matching comments.", example = "100")
    private long total;

    @Schema(description = "Total pages.", example = "5")
    private int totalPages;
}
