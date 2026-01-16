package com.magiccode.backend.service;


import com.magiccode.backend.dto.AddHomeMediaRequest;
import com.magiccode.backend.dto.HomeMediaDto;
import com.magiccode.backend.dto.HomeProfileDto;
import com.magiccode.backend.mapping.HomeMapper;
import com.magiccode.backend.model.HomeMedia;
import com.magiccode.backend.model.HomeProfile;
import com.magiccode.backend.repository.HomeMediaRepository;
import com.magiccode.backend.repository.HomeProfileRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Data
@Transactional
public class HomeService {
    private final HomeProfileRepository homeProfileRepository;
    private final HomeMediaRepository homeMediaRepository;
    private final PostService postService;
    private final HomeMapper homeMapper;


    public HomeProfileDto getHome() {
        HomeProfile home = getOrCreateSingleton();
        return homeMapper.toProfileDto(home, homeMediaRepository);
    }

    public HomeProfileDto updateHome(HomeProfileDto dto) {
        HomeProfile home = getOrCreateSingleton();

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            home.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            home.setContent(dto.getContent());
        }
        if (dto.getCoverImageUrl() != null) {
            home.setCoverImageUrl(dto.getCoverImageUrl());
        }
        if (dto.getCoverVideoUrl() != null) {
            home.setCoverVideoUrl(dto.getCoverVideoUrl());
        }
        homeProfileRepository.save(home);
        return homeMapper.toProfileDto(home, homeMediaRepository);
    }

    public HomeMediaDto uploadHomeImage(MultipartFile file, String caption, Integer orderIndex) {
        HomeProfile home = getOrCreateSingleton();
        String url = postService.uploadImage(file);

        HomeMedia media = addMediaInternal(home, HomeMedia.MediaType.IMAGE, url, caption, orderIndex);
        return homeMapper.toMediaDto(media);
    }

    public HomeMediaDto uploadHomeVideo(MultipartFile file, String caption, Integer orderIndex) {
        HomeProfile home = getOrCreateSingleton();
        String url = postService.uploadVideo(file);

        HomeMedia media = addMediaInternal(home, HomeMedia.MediaType.VIDEO, url, caption, orderIndex);
        return homeMapper.toMediaDto(media);
    }

    public HomeMediaDto addMedia(AddHomeMediaRequest request) {
        HomeProfile home = getOrCreateSingleton();
        if (request.getType() == null || request.getType().isBlank()) {
            throw new RuntimeException("type is required");
        }
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            throw new RuntimeException("url is required");
        }
        HomeMedia.MediaType type;
        try {
            type = HomeMedia.MediaType.valueOf(request.getType().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new RuntimeException("Invalid type, must be IMAGE or VIDEO");
        }
        HomeMedia media = addMediaInternal(home, type, request.getUrl().trim(), request.getCaption(), request.getOrderIndex());
        return homeMapper.toMediaDto(media);
    }

    public void deleteMedia(Long mediaId) {
        homeMediaRepository.deleteByHomeProfileId(mediaId);
    }

    public HomeMediaDto updateMedia(Long mediaId, HomeMediaDto dto) {
        HomeMedia media = homeMediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media Not Found"));
        if (dto.getCaption() != null) media.setCaption(dto.getCaption());
        if (dto.getOrderIndex() != null) media.setOrderIndex(dto.getOrderIndex());
        if (dto.getUrl() != null && !dto.getUrl().isBlank()) media.setUrl(dto.getUrl().trim());

        if (dto.getType() != null && !dto.getType().toString().isBlank()) {
            try {
                media.setType(HomeMedia.MediaType.valueOf(dto.getType().toString().trim().toUpperCase(Locale.ROOT)));
            } catch (Exception e) {
                throw new RuntimeException("Invalid type, must be IMAGE or VIDEO");
            }
        }
        homeMediaRepository.save(media);
        return homeMapper.toMediaDto(media);
    }

    //-----help-tools-----
    private HomeProfile getOrCreateSingleton() {
        return homeProfileRepository.findTopByOrderByIdAsc().orElseGet(() -> {
            HomeProfile created = HomeProfile.builder()
                    .title("Home")
                    .content("")
                    .build();
            return homeProfileRepository.save(created);
        });
    }

    private HomeMedia addMediaInternal(HomeProfile home, HomeMedia.MediaType type, String url, String caption, Integer orderIndex) {
        HomeMedia media=HomeMedia.builder()
                .homeProfile(home)
                .type(type)
                .url(url)
                .caption(caption)
                .orderIndex(orderIndex!=null ? orderIndex:0)
                .build();
        return homeMediaRepository.save(media);
    }
}
