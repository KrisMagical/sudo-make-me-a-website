package com.magiccode.backend.service;

import com.magiccode.backend.mapping.EmbeddedVideoRepository;
import com.magiccode.backend.model.EmbeddedVideo;
import com.magiccode.backend.util.VideoEmbedParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VideoService {
    private final EmbeddedVideoRepository embeddedVideoRepository;

    public void syncFromContent(EmbeddedVideo.OwnerType ownerType, Long ownerId, String content) {
        String safe = content == null ? "" : content;

        embeddedVideoRepository.deleteAllByOwnerTypeAndOwnerId(ownerType, ownerId);

        List<VideoEmbedParser.VideoCandidate> candidates = VideoEmbedParser.parseAll(safe);
        int order = 0;
        for (var c : candidates) {
            EmbeddedVideo v = EmbeddedVideo.builder()
                    .ownerType(ownerType)
                    .ownerId(ownerId)
                    .provider(c.provider())
                    .sourceUrl(c.sourceUrl())
                    .embedUrl(c.embedUrl())
                    .orderIndex(order++)
                    .build();
            embeddedVideoRepository.save(v);
        }
    }

    public List<EmbeddedVideo> list(EmbeddedVideo.OwnerType ownerType, Long ownerId) {
        return embeddedVideoRepository.findAllByOwnerTypeAndOwnerIdOrderByOrderIndexAsc(ownerType, ownerId);
    }

    public void deleteAll(EmbeddedVideo.OwnerType ownerType, Long ownerId) {
        embeddedVideoRepository.deleteAllByOwnerTypeAndOwnerId(ownerType, ownerId);
    }
}
