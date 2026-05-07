package com.magiccode.backend.repository;

import com.magiccode.backend.model.PostGroup;
import com.magiccode.backend.model.PostGroupItem;
import com.magiccode.backend.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostGroupItemRepository extends JpaRepository<PostGroupItem, Long> {
    List<PostGroupItem> findByPostGroupOrderByOrderIndexAsc(PostGroup postGroup);

    Optional<PostGroupItem> findByPostGroupAndPost(PostGroup postGroup, Post post);

    void deleteByPostGroupAndPost(PostGroup postGroup, Post post);

    boolean existsByPostGroupAndPost(PostGroup postGroup, Post post);

    void deleteByPost(Post post);

    @Modifying
    @Query("DELETE FROM PostGroupItem pgi WHERE pgi.postGroup = ?1")
    void deleteByPostGroup(PostGroup postGroup);

    @Modifying
    @Query("DELETE FROM PostGroupItem pgi WHERE pgi.post IN :posts")
    void deleteByPostIn(@Param("posts") List<Post> posts);

    @Query("SELECT p FROM PostGroupItem pgi JOIN pgi.post p WHERE pgi.postGroup.id = :groupId " +
            "AND p.published = true AND p.slug != :draftSlug " +
            "ORDER BY pgi.orderIndex ASC")
    Page<Post> findPostsByGroupId(@Param("groupId") Long groupId,
                                  @Param("draftSlug") String draftSlug,
                                  Pageable pageable);

    @Query("SELECT p FROM PostGroupItem pgi JOIN pgi.post p WHERE pgi.postGroup.id = :groupId " +
            "AND p.published = true AND p.slug != :draftSlug " +
            "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
            "ORDER BY " +
            "(CASE WHEN p.title LIKE %:keyword% THEN 10 ELSE 0 END + " +
            " CASE WHEN p.content LIKE %:keyword% THEN 1 ELSE 0 END) DESC, " +
            "pgi.orderIndex ASC")
    Page<Post> searchPostsByGroupIdAndKeyword(@Param("groupId") Long groupId,
                                              @Param("keyword") String keyword,
                                              @Param("draftSlug") String draftSlug,
                                              Pageable pageable);

    @Query("SELECT pgi.post.id, pg.name FROM PostGroupItem pgi JOIN pgi.postGroup pg WHERE pgi.post.id IN :postIds")
    List<Object[]> findCollectionNamesByPostIds(@Param("postIds") List<Long> postIds);
}
