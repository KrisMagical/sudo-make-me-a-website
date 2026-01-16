package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.SocialDto;
import com.magiccode.backend.model.Social;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SocialMapper {
    @Mapping(source = "id",target = "id")
    @Mapping(source = "name",target = "name")
    @Mapping(source = "url",target = "url")
    @Mapping(source = "description",target = "description")
    @Mapping(source = "iconUrl",target = "iconUrl")
    SocialDto toDto(Social social);

    @Mapping(target = "id",ignore = true)
    @Mapping(source = "name",target = "name")
    @Mapping(source = "url",target = "url")
    @Mapping(source = "description",target = "description")
    @Mapping(source = "iconUrl",target = "iconUrl")
    Social toEntity(SocialDto dto);

    List<SocialDto> toDtoList(List<Social> socials);
}
