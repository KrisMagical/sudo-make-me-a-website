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
@Schema(description = "Public home page profile content.")
public class HomeProfileDto {
    @Schema(description = "Home profile id.", example = "1")
    private Long id;
    @Schema(description = "Home page title.", example = "sudo make me a website")
    private String title;
    @Schema(description = "Home page content.", example = "A small personal blog.")
    private String content;
    @Schema(description = "Home page images.")
    private List<ImageDto> images;
    @Schema(description = "Home page videos.")
    private List<VideoDto> videos;
}
