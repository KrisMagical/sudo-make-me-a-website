package com.magiccode.backend.repository;

import com.magiccode.backend.model.SiteConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteConfigRepository extends JpaRepository<SiteConfig,Long> {
    Optional<SiteConfig> findByIsActiveTrue();
    Optional<SiteConfig> findFirstByOrderByIdDesc();
    boolean existsBySiteName(String siteName);
}
