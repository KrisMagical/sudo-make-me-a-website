package com.magiccode.backend.service;

import com.magiccode.backend.dto.ImageDto;
import com.magiccode.backend.dto.PostGroupDto;
import com.magiccode.backend.dto.PostSummaryDto;
import com.magiccode.backend.mapping.PostGroupMapper;
import com.magiccode.backend.mapping.PostSummaryMapper;
import com.magiccode.backend.model.*;
import com.magiccode.backend.repository.CommentRepository;
import com.magiccode.backend.repository.PostGroupItemRepository;
import com.magiccode.backend.repository.PostGroupRepository;
import com.magiccode.backend.repository.PostRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Data
@Transactional
public class PostGroupService {
    private final PostGroupRepository postGroupRepository;
    private final PostGroupItemRepository postGroupItemRepository;
    private final PostRepository postRepository;
    private final ImageService imageService;
    private final PostSummaryMapper postSummaryMapper;
    private final PostGroupMapper postGroupMapper;
    private final CommentRepository commentRepository;
    private final LikeLogService likeLogService;
    private final VideoService videoService;

    public static final String DRAFT_SLUG = "00100000";

    private PostGroup getOrCreateDraft() {
        return postGroupRepository.findBySlug(DRAFT_SLUG)
                .orElseGet(() -> {
                    PostGroup draft = PostGroup.builder()
                            .name("draft collection")
                            .slug(DRAFT_SLUG)
                            .description("")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return postGroupRepository.save(draft);
                });
    }

    public PostGroupDto create(PostGroupDto dto) {
        if (postGroupRepository.existsBySlug(dto.getSlug())) {
            throw new RuntimeException("Slug already exists");
        }
        PostGroup postGroup = postGroupMapper.toEntity(dto);
        postGroup = postGroupRepository.save(postGroup);
        return buildDto(postGroup);
    }

    public PostGroupDto update(Long id, PostGroupDto dto) {
        PostGroup postGroup = postGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection not found"));

        boolean isDraft = DRAFT_SLUG.equals(postGroup.getSlug());
        boolean becomingReal = dto.getSlug() != null && !dto.getSlug().isBlank()
                && !DRAFT_SLUG.equals(dto.getSlug());

        if (isDraft && becomingReal) {
            if (postGroupRepository.existsBySlug(dto.getSlug())) {
                throw new RuntimeException("Slug already exists, please use another slug.");
            }
            postGroup.setSlug(dto.getSlug());
        }

        if (dto.getName() != null) postGroup.setName(dto.getName());
        if (dto.getDescription() != null) postGroup.setDescription(dto.getDescription());
        if (dto.getCoverImageId() != null) postGroup.setCoverImageId(dto.getCoverImageId());

        if (dto.getSlug() != null && !isDraft && !becomingReal) {
            if (!dto.getSlug().equals(postGroup.getSlug()) && postGroupRepository.existsBySlug(dto.getSlug())) {
                throw new RuntimeException("Slug already exists");
            }
            postGroup.setSlug(dto.getSlug());
        }

        postGroup.setUpdatedAt(LocalDateTime.now());
        postGroup = postGroupRepository.save(postGroup);
        return buildDto(postGroup);
    }

    public List<PostGroupDto> listAll() {
        return postGroupRepository.findAll().stream()
                .filter(pg -> !DRAFT_SLUG.equals(pg.getSlug()))
                .map(this::buildDto)
                .collect(Collectors.toList());
    }

    public PostGroupDto getBySlug(String slug) {
        PostGroup postGroup;
        if (DRAFT_SLUG.equals(slug)) {
            postGroup = getOrCreateDraft();
        } else {
            postGroup = postGroupRepository.findBySlug(slug)
                    .orElseThrow(() -> new RuntimeException("Collection not found"));
        }
        return buildDto(postGroup);
    }

    public void delete(Long id, boolean deletePosts) {
        PostGroup postGroup = postGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collection not found"));
        List<PostGroupItem> pgi = postGroupItemRepository.findByPostGroupOrderByOrderIndexAsc(postGroup);
        List<Post> posts = pgi.stream().map(PostGroupItem::getPost).distinct().toList();
        postGroupItemRepository.deleteByPostGroup(postGroup);

        if (deletePosts) {
            postGroupItemRepository.deleteByPostIn(posts);
            for (Post post : posts) {
                likeLogService.deleteAllByPostId(post.getId());
                imageService.deleteAll(EmbeddedImage.OwnerType.POST, post.getId());
                videoService.deleteAll(EmbeddedVideo.OwnerType.POST, post.getId());
                commentRepository.deleteByPostId(post.getId());
                postRepository.delete(post);
            }
        }
        if (postGroup.getCoverImageId() != null) {
            imageService.delete(EmbeddedImage.OwnerType.COLLECTION, postGroup.getId(), postGroup.getCoverImageId());
        }
        imageService.deleteAll(EmbeddedImage.OwnerType.COLLECTION, postGroup.getId());
        postGroupRepository.delete(postGroup);
    }

    public void addPost(Long postGroupId, Long postId, Integer orderIndex) {
        PostGroup postGroup = postGroupRepository.findById(postGroupId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (postGroupItemRepository.existsByPostGroupAndPost(postGroup, post)) {
            throw new RuntimeException("Post already in collection");
        }

        PostGroupItem pgi = PostGroupItem.builder()
                .postGroup(postGroup)
                .post(post)
                .orderIndex(orderIndex != null ? orderIndex : getNextOrder(postGroup))
                .build();
        postGroupItemRepository.save(pgi);
    }

    public void removePost(Long postGroupId, Long postId) {
        PostGroup postGroup = postGroupRepository.findById(postGroupId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        postGroupItemRepository.deleteByPostGroupAndPost(postGroup, post);
    }

    public void reorderPosts(Long postGroupId, List<Long> orderedPostIds) {
        PostGroup postGroup = postGroupRepository.findById(postGroupId)
                .orElseThrow(() -> new RuntimeException("PostGroup not found"));
        postGroupItemRepository.deleteByPostGroup(postGroup);
        int index = 0;
        for (Long postId : orderedPostIds) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
            PostGroupItem item = PostGroupItem.builder()
                    .postGroup(postGroup)
                    .post(post)
                    .orderIndex(index++)
                    .build();
            postGroupItemRepository.save(item);
        }
    }

    public ImageDto uploadCoverImage(Long postGroupId, MultipartFile file) {
        PostGroup postGroup = postGroupRepository.findById(postGroupId)
                .orElseThrow(() -> new RuntimeException("PostGroup not found"));

        if (postGroup.getCoverImageId() != null) {
            imageService.delete(EmbeddedImage.OwnerType.COLLECTION, postGroup.getId(), postGroup.getCoverImageId());
        }

        ImageDto dto = imageService.uploadToPostGroup(postGroupId, file);
        postGroup.setCoverImageId(dto.getId());
        postGroupRepository.save(postGroup);
        dto.setUrl("/api/images/COLLECTION/" + postGroupId + "/" + dto.getId());
        return dto;
    }

    public List<PostGroupDto> search(String query) {
        return postGroupRepository.findByNameContainingIgnoreCase(query).stream()
                .map(this::buildDto)
                .collect(Collectors.toList());
    }

    private int getNextOrder(PostGroup postGroup) {
        List<PostGroupItem> items = postGroupItemRepository.findByPostGroupOrderByOrderIndexAsc(postGroup);
        return items.stream().mapToInt(PostGroupItem::getOrderIndex).max().orElse(0) + 1;
    }

    private PostGroupDto buildDto(PostGroup postGroup) {
        PostGroupDto dto = postGroupMapper.toDto(postGroup);

        if (postGroup.getCoverImageId() != null) {
            dto.setCoverImageUrl("/api/images/COLLECTION/" + postGroup.getId() + "/" + postGroup.getCoverImageId());
        }
        dto.setImages(imageService.listImages(EmbeddedImage.OwnerType.COLLECTION, postGroup.getId()));

        List<PostGroupItem> items = postGroupItemRepository.findByPostGroupOrderByOrderIndexAsc(postGroup);
        List<PostSummaryDto> postDtos = items.stream()
                .map(item -> postSummaryMapper.toPostSummaryDto(item.getPost()))
                .collect(Collectors.toList());
        dto.setPosts(postDtos);

        return dto;
    }

    public Page<PostSummaryDto> searchPostsInCollection(Long groupId, String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPostsByCollection(groupId, pageable);
        }
        Page<Post> postPage = postGroupItemRepository.searchPostsByGroupIdAndKeyword(groupId, keyword.trim(), DRAFT_SLUG, pageable);
        return postPage.map(postSummaryMapper::toPostSummaryDto);
    }

    public Page<PostSummaryDto> getPostsByCollection(Long groupId, Pageable pageable) {
        Page<Post> postPage = postGroupItemRepository.findPostsByGroupId(groupId, DRAFT_SLUG, pageable);
        return postPage.map(postSummaryMapper::toPostSummaryDto);
    }
}
