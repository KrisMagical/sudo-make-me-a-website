package com.magiccode.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Post summary used in lists and search results.")
public class PostSummaryDto {
    @Schema(description = "Post id.", example = "1")
    private Long id;
    @Schema(description = "Post title.", example = "Hello World")
    private String title;
    @Schema(description = "URL slug.", example = "hello-world")
    private String slug;
    @Schema(description = "Short excerpt.", example = "A small note about building things.")
    private String excerpt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation time.", example = "2026-06-15T08:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "Like count.", example = "3")
    private Integer likeCount;
    @Schema(description = "Dislike count.", example = "0")
    private Integer dislikeCount;
    @Schema(description = "View count.", example = "120")
    private Integer viewCount;
    @Schema(description = "Category name.", example = "Blog")
    private String categoryName;
    @Schema(description = "Collection names containing this post.", example = "[\"Notes\"]")
    private List<String> collectionNames;
}
