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
public class PageTreeNodeDto {
    private Long id;
    private String slug;
    private String title;
    private String content;
    private Long parentId;
    private Integer orderIndex;
    private List<PageTreeNodeDto> children;  // 子页面
    private Boolean hasChildren;  // 是否有子页面
    private Integer depth;  // 深度层级
}
