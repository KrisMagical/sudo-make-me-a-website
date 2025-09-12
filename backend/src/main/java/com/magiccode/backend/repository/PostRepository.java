package com.magiccode.backend.repository;

import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByCategory(Category category);

    Post findBySlug(String slug);
}
