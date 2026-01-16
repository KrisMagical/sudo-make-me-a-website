package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.HomeMediaDto;
import com.magiccode.backend.dto.HomeProfileDto;
import com.magiccode.backend.model.HomeMedia;
import com.magiccode.backend.model.HomeProfile;
import com.magiccode.backend.repository.HomeMediaRepository;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HomeMapper {
    @Mapping(target = "type",source = "type")
    HomeMediaDto toMediaDto(HomeMedia media);

    List<HomeMediaDto> toMediaDtoList(List<HomeMedia> media);

    @Mapping(target = "mediaDtoList", ignore = true)
    HomeProfileDto toProfileDto(HomeProfile home, @Context HomeMediaRepository homeMediaRepository);

    @AfterMapping
    default void fillMediaList(HomeProfile home,
                               @MappingTarget HomeProfileDto homeProfileDto,
                               @Context HomeMediaRepository homeMediaRepository) {
        if (home == null || home.getId() == null) {
            homeProfileDto.setMediaDtoList(List.of());
            return;
        }
        List<HomeMedia> media = homeMediaRepository
                .findByHomeProfileIdOrderByOrderIndexAscIdAsc(home.getId());
        homeProfileDto.setMediaDtoList(toMediaDtoList(media));
    }
}
