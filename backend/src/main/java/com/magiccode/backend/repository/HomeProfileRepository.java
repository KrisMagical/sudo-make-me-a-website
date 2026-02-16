package com.magiccode.backend.repository;

import com.magiccode.backend.model.HomeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HomeProfileRepository extends JpaRepository<HomeProfile, Long> {
    Optional<HomeProfile> findFirstByOrderByIdAsc();
}
