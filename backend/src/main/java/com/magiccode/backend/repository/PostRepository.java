package com.magiccode.backend.repository;

import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCategory(Category category);

    Post findBySlug(String slug);

    List<Post> findByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.published = true AND " +
            "(p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
            "ORDER BY " +
            "(CASE WHEN p.title LIKE %:keyword% THEN 10 ELSE 0 END + " +
            " CASE WHEN p.content LIKE %:keyword% THEN 1 ELSE 0 END) DESC, " +
            "p.createdAt DESC")
    List<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
