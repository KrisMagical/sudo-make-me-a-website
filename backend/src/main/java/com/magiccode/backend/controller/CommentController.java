package com.magiccode.backend.controller;

import com.magiccode.backend.dto.CommentDto;
import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.service.CommentService;
import com.magiccode.backend.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {
    private CommentService commentService;
    private final RateLimitService rateLimitService;
    private final HttpServletRequest httpServletRequest;

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long postId) {
        List<CommentDto> commentDto = commentService.getCommentsByPostId(postId);
        if (commentDto == null) {
            throw new RuntimeException("Comment Not Found.");
        }
        return new ResponseEntity<>(commentDto, HttpStatus.OK);
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long postId, @RequestBody CreateCommentRequest request) {
        String ip = httpServletRequest.getRemoteAddr();
        if (!rateLimitService.tryAcquire(ip)) {
            throw new RuntimeException("Too many comments, please try again later.");
        }

        CommentDto commentDto = commentService.addComment(postId, request);
        if (commentDto == null) {
            throw new RuntimeException("Add Comment Failed");
        }
        return new ResponseEntity<>(commentDto, HttpStatus.CREATED);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentDto> deleteComment(@PathVariable Long commentId, @RequestParam String email) {
        CommentDto commentDto = commentService.deleteComment(commentId, email);
        return new ResponseEntity<>(commentDto, HttpStatus.OK);
    }
}
