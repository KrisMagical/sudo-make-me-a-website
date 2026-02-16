package com.magiccode.backend.repository;

import com.magiccode.backend.model.JwtSecret;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtSecretRepository extends JpaRepository<JwtSecret, Long> {
}

