package com.magiccode.backend.controller;

import com.magiccode.backend.dto.CommentDto;
import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.service.CommentService;
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
