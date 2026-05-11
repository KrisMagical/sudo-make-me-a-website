package com.magiccode.backend.repository;

import com.magiccode.backend.model.LikeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LikeLogRepository extends JpaRepository<LikeLog, Long> {
    int countByPostIdAndPositive(Long postId,boolean positive);

    void deleteAllByPostId(Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LikeLog l WHERE l.post.id IN :postIds")
    void deleteByPostIdIn(@Param("postIds") List<Long> postIds);
}
