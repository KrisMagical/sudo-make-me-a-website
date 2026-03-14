package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageSummaryDto {
    private Long id;
    private String slug;
    private String title;
    private Long parentId;
    private Boolean hasChildren;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
