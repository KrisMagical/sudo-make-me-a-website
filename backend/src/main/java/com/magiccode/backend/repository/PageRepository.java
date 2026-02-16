package com.magiccode.backend.repository;

import com.magiccode.backend.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    Page findBySlug(String slug);

    boolean existsBySlug(String slug);
    List<Page> findBySlugIn(Collection<String> slugs);
    List<Page> findByParent_IdOrderByOrderIndexAsc(Long parentId);

    List<Page> findByParentIsNullOrderByOrderIndexAsc();
}
