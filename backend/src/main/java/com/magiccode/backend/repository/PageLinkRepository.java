package com.magiccode.backend.repository;

import com.magiccode.backend.model.Page;
import com.magiccode.backend.model.PageLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageLinkRepository extends JpaRepository<PageLink, Long> {
    void deleteByFromPage(Page fromPage);

    List<PageLink> findByFromPage(Page fromPage);

    List<PageLink> findByToPage(Page toPage);

    boolean existsByFromPageAndToPage(Page fromPage, Page toPage);

    void deleteByToPage(Page page);
}
