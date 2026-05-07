package com.magiccode.backend.repository;

import com.magiccode.backend.model.MaintenanceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceConfigRepository extends JpaRepository<MaintenanceConfig, Long> {
}
