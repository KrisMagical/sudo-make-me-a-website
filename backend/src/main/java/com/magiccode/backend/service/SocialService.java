package com.magiccode.backend.service;

import com.magiccode.backend.dto.SocialDto;
import com.magiccode.backend.mapping.SocialMapper;
import com.magiccode.backend.model.Social;
import com.magiccode.backend.repository.SocialRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialService {
    private final SocialRepository socialRepository;
    private final SocialMapper socialMapper;

    @Value("${upload.icon.path}")
    private String iconUploadPath;

    public SocialDto create(SocialDto dto, MultipartFile iconFile, String externalIconUrl) {
        if (socialRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Social already exists: " + dto.getName());
        }
        String iconUrl = handleIcon(iconFile, externalIconUrl);

        Social social = socialMapper.toEntity(dto);
        social.setIconUrl(iconUrl);

        socialRepository.save(social);
        return socialMapper.toDto(social);
    }

    public SocialDto update(Long id, SocialDto dto, MultipartFile iconFile, String externalIconUrl) {
        Social social = socialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Social not found"));
        if (dto.getName() != null) social.setName(dto.getName());
        if (dto.getUrl() != null) social.setUrl(dto.getUrl());
        if (dto.getDescription() != null) social.setDescription(dto.getDescription());
        if ((iconFile != null && !iconFile.isEmpty()) || externalIconUrl != null) {
            social.setIconUrl(handleIcon(iconFile, externalIconUrl));
        }
        socialRepository.save(social);
        return socialMapper.toDto(social);
    }

    public void delete(Long id) {
        socialRepository.deleteById(id);
    }

    public List<SocialDto> listAll() {
        return socialMapper.toDtoList(socialRepository.findAll());
    }

    //icon cope logic
    private String handleIcon(MultipartFile file, String externalUrl) {
        if (file != null && !file.isEmpty()) {
            return uploadLocalIcon(file);
        }
        if (externalUrl != null && !externalUrl.isBlank()) {
            return externalUrl;
        }
        throw new RuntimeException("icon is required");
    }

    private String uploadLocalIcon(MultipartFile file) {
        try {
            String original = file.getOriginalFilename();
            String safe = UUID.randomUUID() + "_" + sanitize(original);

            Path path = Paths.get(iconUploadPath, safe);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            String encode = UriUtils.encodePathSegment(safe, StandardCharsets.UTF_8);
            return "/icons/" + encode;
        } catch (IOException e) {
            throw new RuntimeException("Icon upload failed");
        }
    }

    private String sanitize(String name) {
        if (name == null) return "icon.png";
        return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }
}
