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
import com.magiccode.backend.repository.PostRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
    private static final String DRAFT_SLUG = "00100000";

    public Page<PostSummaryDto> getPostByCategorySlug(String slug, Pageable pageable) {
        Category category = categoryRepository.findBySlug(slug);
        if (category == null) {
            throw new RuntimeException("Category Not Found.");
        }
        Page<Post> postPage = postRepository.findByCategory(category, pageable);
        return postPage.map(postSummaryMapper::toPostSummaryDto);
    }

    public PostDetailDto getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug);
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            PostDetailDto dto = postDetailMapper.toPostDetailDto(post);
            dto.setImages(imageService.listPostImages(post.getId()));
            dto.setVideos(videoMapper.toDtoList(videoService.list(EmbeddedVideo.OwnerType.POST, post.getId())));
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


        boolean isDraft = post.getSlug().equals(DRAFT_SLUG);
        boolean becomingReal = updatePostDetailDto.getSlug() != null && !updatePostDetailDto.getSlug().equals(DRAFT_SLUG);

        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug);
            if (category == null) {
                throw new RuntimeException("Category Not Found");
            }
            post.setCategory(category);
        }
        if (updatePostDetailDto.getTitle() != null) {
            post.setTitle(updatePostDetailDto.getTitle());
        }

        if (updatePostDetailDto.getSlug() != null && !updatePostDetailDto.getSlug().equals(post.getSlug())) {
            Post existing = postRepository.findBySlug(updatePostDetailDto.getSlug());
            if (existing != null && !existing.getId().equals(id)) {
                throw new RuntimeException("Slug already exists, please use another slug.");
            }
            post.setSlug(updatePostDetailDto.getSlug());
        }

        if (updatePostDetailDto.getContent() != null) {
            post.setContent(updatePostDetailDto.getContent());
        }

        if (isDraft && becomingReal) {
            post.setCreatedAt(LocalDateTime.now());
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

        return posts.stream()
                .map(postSummaryMapper::toPostSummaryDto)
                .toList();
    }
}