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
import com.magiccode.backend.repository.PostRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<PostSummaryDto> getPostByCategorySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug);
        if (category != null) {
            return postSummaryMapper.toPostSummaryDtoList(postRepository.findByCategory(category));
        } else {
            throw new RuntimeException("Post Not Found.");
        }
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
        if (post == null) {
            throw new RuntimeException("Post Not Found.");
        }
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
        if (updatePostDetailDto.getSlug() != null) {
            post.setSlug(updatePostDetailDto.getSlug());
        }
        if (updatePostDetailDto.getContent() != null) {
            post.setContent(updatePostDetailDto.getContent());
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
        postRepository.delete(post);
    }

    public List<PostSummaryDto> getRecentPosts(int limit) {
        return postRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(postSummaryMapper::toPostSummaryDto)
                .toList();
    }

}