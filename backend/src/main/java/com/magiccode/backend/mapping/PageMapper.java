package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.PageDto;
import com.magiccode.backend.model.Page;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PageMapper {
    PageDto toDto(Page page);

    @Mapping(target = "id", ignore = true)
    Page toEntity(PageDto dto);
}
