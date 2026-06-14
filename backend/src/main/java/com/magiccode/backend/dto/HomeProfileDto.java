package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeProfileDto {
    private Long id;
    private String title;
    private String content;
    private List<ImageDto> images;
    private List<VideoDto> videos;
}
