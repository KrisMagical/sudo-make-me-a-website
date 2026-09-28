package com.magiccode.backend.repository;

import com.magiccode.backend.model.Comment;
import com.magiccode.backend.model.CommentStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {
    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(Long postId, CommentStatus status);

    long countByStatus(CommentStatus status);

    @Query("SELECT c FROM Comment c JOIN c.post p WHERE " +
            "LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.slug) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY c.createdAt DESC")
    List<Comment> searchByKeyword(@Param("keyword") String keyword);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);
}
