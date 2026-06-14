package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.ImageDto;
import com.magiccode.backend.model.EmbeddedImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    ImageDto toDto(EmbeddedImage entity);

    List<ImageDto> toDtoList(List<EmbeddedImage> entities);
}
