package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.PageDto;
import com.magiccode.backend.model.Page;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PageMapper {
    @Mapping(target = "parentId", expression = "java(page.getParent() != null ? page.getParent().getId() : null)")
    PageDto toDto(Page page);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    Page toEntity(PageDto dto);
}
