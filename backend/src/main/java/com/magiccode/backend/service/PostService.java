package com.magiccode.backend.service;

import com.magiccode.backend.dto.PostDetailDto;
import com.magiccode.backend.dto.PostSummaryDto;
import com.magiccode.backend.mapping.PostDetailMapper;
import com.magiccode.backend.mapping.PostSummaryMapper;
import com.magiccode.backend.mapping.VideoMapper;
import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.EmbeddedImage;
import com.magiccode.backend.model.EmbeddedVideo;
import com.magiccode.backend.model.Post;
import com.magiccode.backend.repository.CategoryRepository;
import com.magiccode.backend.repository.CommentRepository;
import com.magiccode.backend.repository.PostGroupItemRepository;
import com.magiccode.backend.repository.PostRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Data
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final PostSummaryMapper postSummaryMapper;
    private final PostDetailMapper postDetailMapper;
    private final LikeLogService likeLogService;
    private final VideoService videoService;
    private final VideoMapper videoMapper;
    private final ImageService imageService;
    private final CommentRepository commentRepository;
    private final PostGroupItemRepository postGroupItemRepository;
    private static final String DRAFT_SLUG = "00100000";

    public Page<PostSummaryDto> getPostByCategorySlug(String slug, Pageable pageable) {
        Category category = categoryRepository.findBySlug(slug);
        if (category == null) {
            throw new RuntimeException("Category Not Found.");
        }
        Page<Post> postPage = postRepository.findByCategoryAndSlugNot(category, DRAFT_SLUG, pageable);
        return postPage.map(postSummaryMapper::toPostSummaryDto);
    }

    public PostDetailDto getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug);
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            PostDetailDto dto = postDetailMapper.toPostDetailDto(post);
            dto.setImages(imageService.listPostImages(post.getId()));
            dto.setVideos(videoMapper.toDtoList(videoService.list(EmbeddedVideo.OwnerType.POST, post.getId())));

            List<Object[]> collectionResults = postGroupItemRepository.findCollectionNamesByPostIds(List.of(post.getId()));
            List<String> collectionNames = collectionResults.stream()
                    .map(row -> (String) row[1])
                    .collect(Collectors.toList());
            dto.setCollectionNames(collectionNames);

            return dto;
        } else {
            throw new RuntimeException("Post Not Found.");
        }
    }

    public PostDetailDto createPost(PostDetailDto postDetailDto, String categorySlug) {
        Category category = categoryRepository.findBySlug(categorySlug);
        if (category == null) {
            throw new RuntimeException("Category Not Found.");
        }
        if (postDetailDto.getSlug() == null || postDetailDto.getSlug().isBlank()) {
            throw new RuntimeException("Slug is Required");
        }
        if (postDetailDto.getTitle() == null || postDetailDto.getTitle().isBlank()) {
            throw new RuntimeException("Title is Required");
        }

        if (postRepository.findBySlug(postDetailDto.getSlug()) != null) {
            throw new RuntimeException("Slug already exists, please use another slug.");
        }

        Post post = postDetailMapper.toPostEntity(postDetailDto);

        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(postDetailDto.getCreatedAt() != null ? postDetailDto.getCreatedAt() : now);
        post.setUpdatedAt(null);

        post.setCategory(category);
        postRepository.save(post);

        videoService.syncFromContent(EmbeddedVideo.OwnerType.POST, post.getId(), post.getContent());
        PostDetailDto dto = postDetailMapper.toPostDetailDto(post);
        dto.setImages(imageService.listPostImages(post.getId()));
        dto.setVideos(videoMapper.toDtoList(videoService.list(EmbeddedVideo.OwnerType.POST, post.getId())));
        return dto;
    }


    public PostDetailDto updatePost(Long id, PostDetailDto updatePostDetailDto, String categorySlug) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post Not Found"));

        boolean contentChanged = false;
        boolean isDraft = post.getSlug().equals(DRAFT_SLUG);
        boolean becomingReal = updatePostDetailDto.getSlug() != null && !updatePostDetailDto.getSlug().equals(DRAFT_SLUG);

        if (updatePostDetailDto.getCreatedAt() != null) {
            post.setCreatedAt(updatePostDetailDto.getCreatedAt());
            contentChanged = true;
        } else if (isDraft && becomingReal) {
            post.setCreatedAt(LocalDateTime.now());
            contentChanged = true;
        }

        if (updatePostDetailDto.getTitle() != null && !updatePostDetailDto.getTitle().equals(post.getTitle())) {
            post.setTitle(updatePostDetailDto.getTitle());
            contentChanged = true;
        }

        if (updatePostDetailDto.getSlug() != null && !updatePostDetailDto.getSlug().isBlank()) {
            String newSlug = updatePostDetailDto.getSlug().trim();
            if (!newSlug.equals(post.getSlug())) {
                Post existing = postRepository.findBySlug(newSlug);
                if (existing != null && !existing.getId().equals(id)) {
                    throw new RuntimeException("Slug already exists, please use another slug.");
                }
                post.setSlug(newSlug);
                contentChanged = true;
            }
        }

        if (updatePostDetailDto.getContent() != null && !updatePostDetailDto.getContent().equals(post.getContent())) {
            post.setContent(updatePostDetailDto.getContent());
            contentChanged = true;
        }

        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug);
            if (category == null) {
                throw new RuntimeException("Category Not Found");
            }
            if (!category.equals(post.getCategory())) {
                post.setCategory(category);
                contentChanged = true;
            }
        }

        if (contentChanged) {
            post.setUpdatedAt(LocalDateTime.now());
        }

        postRepository.save(post);
        videoService.syncFromContent(EmbeddedVideo.OwnerType.POST, post.getId(), post.getContent());
        PostDetailDto dto = postDetailMapper.toPostDetailDto(post);
        dto.setImages(imageService.listPostImages(post.getId()));
        dto.setVideos(videoMapper.toDtoList(videoService.list(EmbeddedVideo.OwnerType.POST, post.getId())));
        return dto;
    }


    public void deletePostBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new RuntimeException("Slug is Required");
        }
        Post post = postRepository.findBySlug(slug);
        if (post == null) {
            throw new RuntimeException("Post Not Found");
        }
        Long postId = post.getId();
        postGroupItemRepository.deleteByPost(post);

        likeLogService.deleteAllByPostId(postId);
        imageService.deleteAll(EmbeddedImage.OwnerType.POST, postId);
        videoService.deleteAll(EmbeddedVideo.OwnerType.POST, postId);
        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    public List<PostSummaryDto> getRecentPosts(int limit) {
        return postRepository.findBySlugNotOrderByCreatedAtDesc(DRAFT_SLUG, PageRequest.of(0, limit))
                .stream()
                .map(postSummaryMapper::toPostSummaryDto)
                .toList();
    }

    public List<PostSummaryDto> searchPosts(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Post> posts = postRepository.searchByKeyword(keyword.trim(), PageRequest.of(0, limit));
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        List<Object[]> collectionResults = postGroupItemRepository.findCollectionNamesByPostIds(postIds);

        Map<Long, List<String>> postCollectionsMap = new HashMap<>();
        for (Object[] row : collectionResults) {
            Long postId = (Long) row[0];
            String collectionName = (String) row[1];
            postCollectionsMap.computeIfAbsent(postId, k -> new ArrayList<>()).add(collectionName);
        }

        return posts.stream()
                .map(post -> {
                    PostSummaryDto dto = postSummaryMapper.toPostSummaryDto(post);
                    dto.setCollectionNames(postCollectionsMap.getOrDefault(post.getId(), Collections.emptyList()));
                    return dto;
                })
                .toList();
    }

    public Page<PostSummaryDto> searchPostsByCategorySlug(String slug, String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty(pageable);
        }
        Category category = categoryRepository.findBySlug(slug);
        if (category == null) {
            throw new RuntimeException("Category Not Found.");
        }
        Page<Post> postPage = postRepository.searchByCategorySlugAndKeyword(slug, keyword.trim(), DRAFT_SLUG, pageable);
        return postPage.map(postSummaryMapper::toPostSummaryDto);
    }
}
