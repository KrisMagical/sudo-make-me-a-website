package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.*;
import com.magiccode.backend.model.BrowserIcon;
import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.Page;
import com.magiccode.backend.model.SiteConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring",uses = {CategoryMapper.class})
public interface SidebarMapper {
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "hasChildren", ignore = true)
    @Mapping(target = "depth", ignore = true)
    @Mapping(target = "parentId", source = "parent.id")
    PageTreeNodeDto toPageTreeNodeDto(Page page);

    default List<PageTreeNodeDto> buildPageTree(List<Page> allPages) {
        List<Page> rootPages = allPages.stream()
                .filter(page -> page.getParent() == null)
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .toList();

        return rootPages.stream()
                .map(rootPage -> buildTreeNode(rootPage, allPages, 0))
                .toList();
    }

    default PageTreeNodeDto buildTreeNode(Page page, List<Page> allPages, int depth) {
        PageTreeNodeDto node = toPageTreeNodeDto(page);
        node.setDepth(depth);

        List<Page> children = allPages.stream()
                .filter(p -> p.getParent() != null && p.getParent().getId().equals(page.getId()))
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .toList();

        node.setHasChildren(!children.isEmpty());
        if (!children.isEmpty()) {
            List<PageTreeNodeDto> childNodes = children.stream()
                    .map(child -> buildTreeNode(child, allPages, depth + 1))
                    .toList();
            node.setChildren(childNodes);
        } else {
            node.setChildren(List.of());
        }

        return node;
    }
}
