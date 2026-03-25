package com.magiccode.backend.service;

import com.magiccode.backend.dto.CommentDto;
import com.magiccode.backend.dto.CommentSearchResultDto;
import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.mapping.CommentMapper;
import com.magiccode.backend.model.Comment;
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
        if (post != null) {
            return commentMapper.toCommentDtoList(post.getComments());
        } else {
            throw new RuntimeException("Comment Not Found");
        }
    }

    public CommentDto addComment(Long postId, CreateCommentRequest request, boolean isAdmin) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post Not Found"));

        Comment comment = commentMapper.toCommentEntity(request);
        comment.setPost(post);

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            if (!parent.getPost().getId().equals(postId)) {
                throw new RuntimeException("Parent comment does not belong to this post");
            }
            comment.setParent(parent);
        }

        comment.setAuthor(isAdmin);

        commentRepository.save(comment);
        return commentMapper.toCommentDto(comment);
    }

    public CommentDto deleteComment(Long commentId, String email) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment Not Found"));
        if (comment == null) {
            throw new RuntimeException("Comment Not Found");
        }
        if (!comment.getEmail().equals(email)) {
            throw new RuntimeException("You are not allowed to delete this comment");
        }
        commentRepository.delete(comment);
        return commentMapper.toCommentDto(comment);
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
