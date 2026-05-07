package com.magiccode.backend.repository;

import com.magiccode.backend.model.PostGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostGroupRepository extends JpaRepository<PostGroup, Long> {
    Optional<PostGroup> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<PostGroup> findByNameContainingIgnoreCase(String keyword);
}
