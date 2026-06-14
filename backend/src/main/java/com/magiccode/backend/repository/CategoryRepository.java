package com.magiccode.backend.repository;

import com.magiccode.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);


    Optional<Category> findByName(String name);
}
