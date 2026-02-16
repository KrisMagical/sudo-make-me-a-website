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
public class PageDto {
    private Long id;
    private String slug;
    private String title;
    private String content;
    private Long parentId;
    private Integer orderIndex;
    private List<ImageDto> images;
    private List<VideoDto> videos;
}
