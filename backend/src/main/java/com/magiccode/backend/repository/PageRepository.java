package com.magiccode.backend.repository;

import com.magiccode.backend.model.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    Page findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Page> findBySlugIn(Collection<String> slugs);

    List<Page> findByParentId(Long parentId);

    List<Page> findByParentIdIsNull();

    @Query(value = "SELECT * FROM pages p WHERE " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(p.content AS CHAR)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY " +
            "CASE WHEN LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 10 ELSE 0 END + " +
            "CASE WHEN LOWER(CAST(p.content AS CHAR)) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 1 ELSE 0 END DESC, " +
            "p.updated_at DESC",
            nativeQuery = true)
    List<Page> searchByKeyword(@Param("keyword") String keyword);

    List<Page> findByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByParent_Id(Long parentId);
}
