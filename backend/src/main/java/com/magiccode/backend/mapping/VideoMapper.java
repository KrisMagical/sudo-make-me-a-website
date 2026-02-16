package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.VideoDto;
import com.magiccode.backend.model.EmbeddedVideo;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoMapper {
    VideoDto toDto(EmbeddedVideo entity);

    List<VideoDto> toDtoList(List<EmbeddedVideo> entities);
}
