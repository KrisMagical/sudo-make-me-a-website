package com.magiccode.backend.service;

import com.magiccode.backend.dto.CommentDto;
import com.magiccode.backend.dto.CommentSearchResultDto;
import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.mapping.CommentMapper;
import com.magiccode.backend.model.Comment;
import com.magiccode.backend.model.CommentStatus;
import com.magiccode.backend.model.Post;
import com.magiccode.backend.repository.CommentRepository;
import com.magiccode.backend.repository.PostRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Data
@Transactional
public class CommentService {
    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private CommentMapper commentMapper;

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
        comment.setStatus(isAdmin ? CommentStatus.APPROVED : CommentStatus.PENDING);

        commentRepository.save(comment);
        return commentMapper.toCommentDto(comment);
    }

    public CommentDto deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment Not Found"));
        commentRepository.delete(comment);
        return commentMapper.toCommentDto(comment);
    }

    public CommentDto updateStatus(Long commentId, CommentStatus status) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment Not Found"));
        comment.setStatus(status);
        return commentMapper.toCommentDto(commentRepository.save(comment));
    }

    public List<CommentSearchResultDto> searchComments(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Comment> comments = commentRepository.searchByKeyword(keyword.trim());
        List<CommentSearchResultDto> results = new ArrayList<>();

        for (Comment comment : comments) {
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
                    .build();

            if (comment.getParent() != null) {
                Comment parent = comment.getParent();
                dto.setParentExists(true);
                dto.setParentName(parent.getName());
                dto.setParentContent(parent.getContent());
            } else {
                dto.setParentExists(false);
            }

            results.add(dto);
        }

        return results;
    }
}
