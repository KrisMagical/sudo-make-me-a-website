package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.HomeProfileDto;
import com.magiccode.backend.model.HomeProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HomeMapper {
    HomeProfileDto toDto(HomeProfile entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    HomeProfile toEntity(HomeProfileDto dto);
}
