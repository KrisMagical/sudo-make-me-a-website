package com.magiccode.backend.repository;

import com.magiccode.backend.model.BrowserIcon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrowserIconRepository extends JpaRepository<BrowserIcon,Long> {
    Optional<BrowserIcon> findByIsActiveTrue();
    Optional<BrowserIcon> findFirstByOrderByIdDesc();
}
