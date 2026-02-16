package com.magiccode.backend.mapping;

import com.magiccode.backend.model.EmbeddedVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmbeddedVideoRepository extends JpaRepository<EmbeddedVideo,Long> {
    List<EmbeddedVideo> findAllByOwnerTypeAndOwnerIdOrderByOrderIndexAsc(EmbeddedVideo.OwnerType ownerType, Long ownerId);

    void deleteAllByOwnerTypeAndOwnerId(EmbeddedVideo.OwnerType ownerType, Long ownerId);
}
