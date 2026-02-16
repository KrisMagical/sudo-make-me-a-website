package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.SocialDto;
import com.magiccode.backend.model.Social;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SocialMapper {

    @Mapping(target = "iconUrl", ignore = true)
    SocialDto toDto(Social social);

    @Mapping(target = "id", ignore = true)
    Social toEntity(SocialDto dto);

    List<SocialDto> toDtoList(List<Social> socials);
}
