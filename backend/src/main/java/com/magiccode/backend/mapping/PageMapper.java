package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.PageDto;
import com.magiccode.backend.dto.PageSummaryDto;
import com.magiccode.backend.model.Page;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PageMapper {
    @Mapping(target = "parentId", expression = "java(page.getParent() != null ? page.getParent().getId() : null)")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    PageDto toDto(Page page);

    @Mapping(target = "parentId", expression = "java(page.getParent() != null ? page.getParent().getId() : null)")
    @Mapping(target = "hasChildren", ignore = true)
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "excerpt", expression = "java(page.getContent() != null && page.getContent().length() > 150 ? page.getContent().substring(0, 150) + \"...\" : page.getContent())")
    PageSummaryDto toSummaryDto(Page page);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    Page toEntity(PageDto dto);
}
