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
public class SidebarDto {
    private SiteConfigDto siteConfig;  // 网站配置信息
    private List<PageTreeNodeDto> pages;  // 页面树形结构
    private List<CategoryDto> categories;  // 分类列表
    private BrowserIconDto browserIcon;  // 浏览器图标
}
