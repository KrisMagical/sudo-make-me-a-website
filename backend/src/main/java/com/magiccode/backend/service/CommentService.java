package com.magiccode.backend.service;

import com.magiccode.backend.dto.CommentDto;
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

import java.util.List;

@Service
@AllArgsConstructor
@Data
@Transactional
public class CommentService {
    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private CommentMapper commentMapper;
    private NotificationService notificationService;

    public List<CommentDto> getCommentsByPostId(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post Not Found"));
        if (post != null) {
            return commentMapper.toCommentDtoList(post.getComments());
        } else {
            throw new RuntimeException("Comment Not Found");
        }
    }

    public CommentDto addComment(Long postId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post Not Found"));
        if (post == null) {
            throw new RuntimeException("Post Not Found.");
        }
        Comment comment = commentMapper.toCommentEntity(request);
        comment.setPost(post);
        commentRepository.save(comment);
        notificationService.sendCommentNotification(comment);
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
}
