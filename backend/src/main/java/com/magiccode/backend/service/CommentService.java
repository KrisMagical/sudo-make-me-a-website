package com.magiccode.backend.service;

import com.magiccode.backend.dto.AdminCommentPageResponse;
import com.magiccode.backend.dto.BulkCommentAction;
import com.magiccode.backend.dto.BulkCommentRequest;
import com.magiccode.backend.dto.BulkCommentResponse;
import com.magiccode.backend.dto.CommentDto;
import com.magiccode.backend.dto.CommentSearchResultDto;
import com.magiccode.backend.dto.CommentStatsDto;
import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.mapping.CommentMapper;
import com.magiccode.backend.model.Comment;
import com.magiccode.backend.model.CommentStatus;
import com.magiccode.backend.model.Post;
import com.magiccode.backend.repository.CommentRepository;
import com.magiccode.backend.repository.PostRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
@Data
@Transactional
public class CommentService {
    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private CommentMapper commentMapper;
    private CommentModerationService commentModerationService;

    public List<CommentDto> getCommentsByPostId(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post Not Found"));
        return commentMapper.toCommentDtoList(
                commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), CommentStatus.APPROVED)
        );
    }

    public CommentDto addComment(Long postId, CreateCommentRequest request, boolean isAdmin) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post Not Found"));

        Comment comment = commentMapper.toCommentEntity(request);
        comment.setPost(post);
        comment.setName(request.getName().trim());
        comment.setEmail(request.getEmail().trim());
        comment.setContent(request.getContent().trim());

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            if (!parent.getPost().getId().equals(postId)) {
                throw new RuntimeException("Parent comment does not belong to this post");
            }
            comment.setParent(parent);
        }

        comment.setAuthor(isAdmin);
        if (isAdmin) {
            comment.setStatus(CommentStatus.APPROVED);
            comment.setModerationReason(null);
        } else {
            CommentModerationService.ModerationResult moderation = commentModerationService.review(request);
            comment.setStatus(moderation.status());
            comment.setModerationReason(moderation.reason());
        }

        commentRepository.save(comment);
        log.info("comment submitted postId={} status={} moderated={} admin={}",
                postId, comment.getStatus(), comment.getModerationReason() != null, isAdmin);
        return commentMapper.toCommentDto(comment);
    }

    public CommentDto deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment Not Found"));
        Long postId = comment.getPost() != null ? comment.getPost().getId() : null;
        commentRepository.delete(comment);
        log.info("comment deleted commentId={} postId={}", commentId, postId);
        return commentMapper.toCommentDto(comment);
    }

    public CommentDto updateStatus(Long commentId, CommentStatus status) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment Not Found"));
        comment.setStatus(status);
        if (status == CommentStatus.APPROVED) {
            comment.setModerationReason(null);
        }
        Comment saved = commentRepository.save(comment);
        log.info("comment status updated commentId={} status={}", commentId, status);
        return commentMapper.toCommentDto(saved);
    }

    public AdminCommentPageResponse listAdminComments(String status, String keyword, Long postId, int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "createdAt asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, "createdAt"));
        Page<Comment> comments = commentRepository.findAll(buildAdminCommentSpec(status, keyword, postId), pageable);

        return AdminCommentPageResponse.builder()
                .items(comments.getContent().stream().map(this::toSearchResultDto).toList())
                .page(comments.getNumber())
                .size(comments.getSize())
                .total(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .build();
    }

    public CommentStatsDto getStats() {
        long pending = commentRepository.countByStatus(CommentStatus.PENDING);
        long approved = commentRepository.countByStatus(CommentStatus.APPROVED);
        long rejected = commentRepository.countByStatus(CommentStatus.REJECTED);
        return CommentStatsDto.builder()
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .total(pending + approved + rejected)
                .build();
    }

    public BulkCommentResponse bulkAction(BulkCommentRequest request) {
        List<Long> ids = request.getCommentIds().stream().distinct().toList();
        List<Comment> comments = commentRepository.findAllById(ids);
        Set<Long> foundIds = comments.stream().map(Comment::getId).collect(java.util.stream.Collectors.toSet());
        List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new RuntimeException("Comments not found: " + missingIds);
        }

        if (request.getAction() == BulkCommentAction.DELETE) {
            commentRepository.deleteAll(comments);
        } else {
            CommentStatus status = request.getAction() == BulkCommentAction.APPROVE
                    ? CommentStatus.APPROVED
                    : CommentStatus.REJECTED;
            for (Comment comment : comments) {
                comment.setStatus(status);
                if (status == CommentStatus.APPROVED) {
                    comment.setModerationReason(null);
                }
            }
            commentRepository.saveAll(comments);
        }
        log.info("comment bulk action action={} count={}", request.getAction(), comments.size());

        return BulkCommentResponse.builder()
                .action(request.getAction())
                .affected(comments.size())
                .build();
    }

    public List<CommentSearchResultDto> searchComments(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Comment> comments = commentRepository.searchByKeyword(keyword.trim());
        List<CommentSearchResultDto> results = new ArrayList<>();

        for (Comment comment : comments) {
            results.add(toSearchResultDto(comment));
        }

        return results;
    }

    private Specification<Comment> buildAdminCommentSpec(String status, String keyword, Long postId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status == null || status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), CommentStatus.PENDING));
            } else if (!"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), CommentStatus.valueOf(status.toUpperCase(Locale.ROOT))));
            }

            if (postId != null) {
                predicates.add(cb.equal(root.get("post").get("id"), postId));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Comment, Post> post = root.join("post");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern),
                        cb.like(cb.lower(post.get("title")), pattern),
                        cb.like(cb.lower(post.get("slug")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private CommentSearchResultDto toSearchResultDto(Comment comment) {
        Post post = comment.getPost();

        CommentSearchResultDto dto = CommentSearchResultDto.builder()
                .id(comment.getId())
                .name(comment.getName())
                .email(comment.getEmail())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .author(comment.isAuthor())
                .status(comment.getStatus())
                .postId(post.getId())
                .postTitle(post.getTitle())
                .postSlug(post.getSlug())
                .moderationReason(comment.getModerationReason())
                .build();

        if (comment.getParent() != null) {
            Comment parent = comment.getParent();
            dto.setParentExists(true);
            dto.setParentName(parent.getName());
            dto.setParentContent(parent.getContent());
        } else {
            dto.setParentExists(false);
        }

        return dto;
    }
}
