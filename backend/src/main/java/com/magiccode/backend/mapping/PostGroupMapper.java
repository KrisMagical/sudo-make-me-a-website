package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.PostGroupDto;
import com.magiccode.backend.model.PostGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PostGroupMapper {
    @Mapping(target = "coverImageUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "posts", ignore = true)
    PostGroupDto toDto(PostGroup entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PostGroup toEntity(PostGroupDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "coverImageId", ignore = true)
    void updateEntityFromDto(PostGroupDto dto, @MappingTarget PostGroup entity);
}
