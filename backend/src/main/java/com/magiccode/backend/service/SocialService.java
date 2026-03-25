package com.magiccode.backend.service;

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

        ImageService.ProcessedFile pf;
        if (iconFile != null && !iconFile.isEmpty()) {
            pf = imageService.processFile(iconFile);
        } else {
            pf = null;
        }

        return transactionTemplate.execute(status -> {
            Social social = socialMapper.toEntity(dto);
            social = socialRepository.save(social);

            if (pf != null) {
                EmbeddedImage saved = imageService.saveImage(EmbeddedImage.OwnerType.SOCIAL, social.getId(),
                        pf.data, pf.originalFilename, pf.contentType, pf.size);
                social.setIconImageId(saved.getId());
                social.setIconUrl(resolveIconUrl(social));
            } else if (externalIconUrl != null && !externalIconUrl.isBlank()) {
                social.setExternalIconUrl(externalIconUrl.trim());
                social.setIconUrl(resolveIconUrl(social));
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

        ImageService.ProcessedFile pf;
        if (hasFile) {
            pf = imageService.processFile(iconFile);
        } else {
            pf = null;
        }

        return transactionTemplate.execute(status -> {
            Social social = socialRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Social not found"));

            if (dto.getName() != null) social.setName(dto.getName());
            if (dto.getUrl() != null) social.setUrl(dto.getUrl());
            if (dto.getDescription() != null) social.setDescription(dto.getDescription());

            boolean changeIcon = hasFile || hasExternal;
            if (changeIcon) {
                if (social.getIconImageId() != null) {
                    imageService.delete(EmbeddedImage.OwnerType.SOCIAL, social.getId(), social.getIconImageId());
                    social.setIconImageId(null);
                }
                social.setExternalIconUrl(null);
                social.setIconUrl(null);

                if (hasFile) {
                    EmbeddedImage saved = imageService.saveImage(EmbeddedImage.OwnerType.SOCIAL, social.getId(),
                            pf.data, pf.originalFilename, pf.contentType, pf.size);
                    social.setIconImageId(saved.getId());
                } else if (hasExternal) {
                    social.setExternalIconUrl(externalIconUrl.trim());
                }
                social.setIconUrl(resolveIconUrl(social));
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