package com.magiccode.backend.service;

import com.magiccode.backend.dto.SocialDto;
import com.magiccode.backend.mapping.SocialMapper;
import com.magiccode.backend.model.EmbeddedImage;
import com.magiccode.backend.model.Social;
import com.magiccode.backend.repository.SocialRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SocialService {
    private final SocialRepository socialRepository;
    private final SocialMapper socialMapper;
    private final ImageService imageService;

    public SocialDto create(SocialDto dto, MultipartFile iconFile, String externalIconUrl) {
        if (socialRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Social already exists: " + dto.getName());
        }

        Social social = socialMapper.toEntity(dto);
        socialRepository.save(social);

        applyIcon(social, iconFile, externalIconUrl);

        socialRepository.save(social);
        return toDtoWithResolvedIcon(social);
    }

    public SocialDto update(Long id, SocialDto dto, MultipartFile iconFile, String externalIconUrl) {
        Social social = socialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Social not found"));
        if (dto.getName() != null && !dto.getName().equals(social.getName())) {
            if (socialRepository.existsByName(dto.getName())) {
                throw new RuntimeException("Social name already exists: " + dto.getName());
            }
        }
        if (dto.getName() != null) social.setName(dto.getName());
        if (dto.getUrl() != null) social.setUrl(dto.getUrl());
        if (dto.getDescription() != null) social.setDescription(dto.getDescription());

        if ((iconFile != null && !iconFile.isEmpty()) || (externalIconUrl != null && !externalIconUrl.isBlank())) {
            applyIcon(social, iconFile, externalIconUrl);
        }

        socialRepository.save(social);
        return toDtoWithResolvedIcon(social);
    }

    public void delete(Long id) {
        Social social = socialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Social not found"));

        if (social.getIconImageId() != null) {
            imageService.delete(EmbeddedImage.OwnerType.SOCIAL, social.getId(), social.getIconImageId());
        } else {
            imageService.deleteAll(EmbeddedImage.OwnerType.SOCIAL, social.getId());
        }

        socialRepository.deleteById(id);
    }

    public List<SocialDto> listAll() {
        return socialRepository.findAll().stream()
                .map(this::toDtoWithResolvedIcon)
                .toList();
    }

    // -------------------- icon logic --------------------
    private void applyIcon(Social social, MultipartFile iconFile, String externalIconUrl) {
        boolean hasFile = iconFile != null && !iconFile.isEmpty();
        boolean hasExternal = externalIconUrl != null && !externalIconUrl.isBlank();

        if (!hasFile && !hasExternal) {
            return;
        }

        if (social.getIconImageId() != null) {
            imageService.delete(EmbeddedImage.OwnerType.SOCIAL, social.getId(), social.getIconImageId());
            social.setIconImageId(null);
        }

        social.setExternalIconUrl(null);
        social.setIconUrl(null);

        if (hasFile) {
            var saved = imageService.uploadToSocial(social.getId(), iconFile);
            social.setIconImageId(saved.getId());
            social.setIconUrl(resolveIconUrl(social));
            return;
        }

        if (hasExternal) {
            social.setExternalIconUrl(externalIconUrl.trim());
            social.setIconUrl(resolveIconUrl(social));
        }
    }

    private SocialDto toDtoWithResolvedIcon(Social social) {
        SocialDto dto = socialMapper.toDto(social);
        dto.setIconImageId(social.getIconImageId());
        dto.setExternalIconUrl(social.getExternalIconUrl());
        dto.setIconUrl(resolveIconUrl(social));
        return dto;
    }

    private String resolveIconUrl(Social social) {
        if (social.getIconImageId() != null) {
            return "/api/images/" + EmbeddedImage.OwnerType.SOCIAL + "/" + social.getId() + "/" + social.getIconImageId();
        }
        if (social.getExternalIconUrl() != null && !social.getExternalIconUrl().isBlank()) {
            return social.getExternalIconUrl();
        }
        return null;
    }
}
