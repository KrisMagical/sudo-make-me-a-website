package com.magiccode.backend.service;


import com.magiccode.backend.dto.HomeProfileDto;
import com.magiccode.backend.mapping.HomeMapper;
import com.magiccode.backend.mapping.VideoMapper;
import com.magiccode.backend.model.EmbeddedVideo;
import com.magiccode.backend.model.HomeProfile;
import com.magiccode.backend.repository.HomeProfileRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Data
@Transactional
public class HomeService {
    private final HomeProfileRepository homeProfileRepository;
    private final HomeMapper homeMapper;
    private final VideoService videoService;
    private final VideoMapper videoMapper;
    private final ImageService imageService;

    public HomeProfileDto getHome() {
        HomeProfile home = ensureHomeExists();
        HomeProfileDto dto = homeMapper.toDto(home);
        dto.setImages(imageService.listHomeImages());
        dto.setVideos(videoMapper.toDtoList(videoService.list(EmbeddedVideo.OwnerType.HOME, home.getId())));
        return dto;
    }

    public HomeProfileDto updateHome(HomeProfileDto dto) {
        HomeProfile home = ensureHomeExists();

        if (dto.getTitle() != null) home.setTitle(dto.getTitle());
        if (dto.getContent() != null) home.setContent(dto.getContent());

        home.setUpdatedAt(LocalDateTime.now());

        homeProfileRepository.save(home);
        videoService.syncFromContent(EmbeddedVideo.OwnerType.HOME, home.getId(), home.getContent());

        return getHome();
    }

    private HomeProfile ensureHomeExists() {
        return homeProfileRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> homeProfileRepository.save(
                        HomeProfile.builder()
                                .title("Home")
                                .content("")
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));
    }
}
