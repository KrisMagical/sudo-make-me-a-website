package com.magiccode.backend.repository;

import com.magiccode.backend.model.Social;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialRepository extends JpaRepository<Social, Long> {
    boolean existsByName(String name);
}
