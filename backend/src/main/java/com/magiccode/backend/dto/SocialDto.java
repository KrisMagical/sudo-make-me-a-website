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
@Schema(description = "Social link shown on the public site.")
public class SocialDto {
    @Schema(description = "Social link id.", example = "1")
    private Long id;
    @Schema(description = "Display name.", example = "GitHub")
    private String name;
    @Schema(description = "Target URL.", example = "https://example.com/profile")
    private String url;
    @Schema(description = "Short description.", example = "Code and notes.")
    private String description;
    @Schema(description = "Resolved icon URL.", example = "https://cdn.example.test/icons/github.svg")
    private String iconUrl;
    @Schema(description = "Uploaded icon image id.", example = "7", nullable = true)
    private Long iconImageId;
    @Schema(description = "External icon URL when no uploaded icon is used.", example = "https://example.com/icon.svg", nullable = true)
    private String externalIconUrl;
}
