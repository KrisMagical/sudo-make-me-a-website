package com.magiccode.backend.service;

import com.magiccode.backend.dto.ImageDto;
import com.magiccode.backend.dto.SocialDto;
import com.magiccode.backend.mapping.SocialMapper;
import com.magiccode.backend.model.EmbeddedImage;
import com.magiccode.backend.model.Social;
import com.magiccode.backend.repository.SocialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialService {
    private final SocialRepository socialRepository;
    private final SocialMapper socialMapper;
    private final ImageService imageService;
    private final TransactionTemplate transactionTemplate;

    public SocialDto create(SocialDto dto, MultipartFile iconFile, String externalIconUrl) {
        if (socialRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Social already exists: " + dto.getName());
        }
        return transactionTemplate.execute(status -> {
            Social social = socialMapper.toEntity(dto);
            social = socialRepository.save(social);

            if (iconFile != null || !iconFile.isEmpty()) {
                ImageDto imageDto = imageService.uploadToSocial(social.getId(), iconFile);
                social.setIconImageId(imageDto.getId());
                social.setIconUrl(imageDto.getUrl());
                social.setExternalIconUrl(null);
            } else if (externalIconUrl != null || !externalIconUrl.isBlank()) {
                social.setExternalIconUrl(externalIconUrl.trim());
                social.setIconUrl(externalIconUrl.trim());
                social.setIconImageId(null);
            }

            socialRepository.save(social);
            return toDtoWithResolvedIcon(social);
        });
    }

    public SocialDto update(Long id, SocialDto dto, MultipartFile iconFile, String externalIconUrl) {
        if (dto.getName() != null && !dto.getName().isBlank()) {
            Social existing = socialRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Social not found"));
            if (!existing.getName().equals(dto.getName()) && socialRepository.existsByName(dto.getName())) {
                throw new RuntimeException("Social name already exists: " + dto.getName());
            }
        }

        boolean hasFile = iconFile != null && !iconFile.isEmpty();
        boolean hasExternal = externalIconUrl != null && !externalIconUrl.isBlank();

        return transactionTemplate.execute(status -> {
            Social social = socialRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Social not found"));

            if (dto.getName() != null) social.setName(dto.getName());
            if (dto.getUrl() != null) social.setUrl(dto.getUrl());
            if (dto.getDescription() != null) social.setDescription(dto.getDescription());

            if (hasFile || hasExternal) {
                if (social.getIconImageId() != null) {
                    imageService.delete(EmbeddedImage.OwnerType.SOCIAL, social.getId(), social.getIconImageId());
                    social.setIconImageId(null);
                }
                social.setExternalIconUrl(null);
                social.setIconUrl(null);

                if (hasFile) {
                    ImageDto imageDto = imageService.uploadToSocial(social.getId(), iconFile);
                    social.setIconImageId(imageDto.getId());
                    social.setIconUrl(imageDto.getUrl());
                } else if (hasExternal) {
                    social.setExternalIconUrl(externalIconUrl.trim());
                    social.setIconUrl(externalIconUrl.trim());
                }
            }

            socialRepository.save(social);
            return toDtoWithResolvedIcon(social);
        });
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

    private SocialDto toDtoWithResolvedIcon(Social social) {
        SocialDto dto = socialMapper.toDto(social);
        dto.setIconImageId(social.getIconImageId());
        dto.setExternalIconUrl(social.getExternalIconUrl());
        dto.setIconUrl(social.getIconUrl());
        return dto;
    }
}