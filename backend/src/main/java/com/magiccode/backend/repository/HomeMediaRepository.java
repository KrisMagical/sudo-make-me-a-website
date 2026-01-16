package com.magiccode.backend.repository;

import com.magiccode.backend.model.HomeMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeMediaRepository extends JpaRepository<HomeMedia, Long> {
    List<HomeMedia> findByHomeProfileIdOrderByOrderIndexAscIdAsc(Long homeProfileId);

    void deleteByHomeProfileId(Long homeProfileId);
}
