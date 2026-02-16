package com.magiccode.backend.repository;

import com.magiccode.backend.model.EmbeddedImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmbeddedImageRepository extends JpaRepository<EmbeddedImage,Long> {
    List<EmbeddedImage> findAllByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(EmbeddedImage.OwnerType ownerType, Long ownerId);

    Optional<EmbeddedImage> findByIdAndOwnerTypeAndOwnerId(Long id, EmbeddedImage.OwnerType ownerType, Long ownerId);

    void deleteAllByOwnerTypeAndOwnerId(EmbeddedImage.OwnerType ownerType, Long ownerId);
}
