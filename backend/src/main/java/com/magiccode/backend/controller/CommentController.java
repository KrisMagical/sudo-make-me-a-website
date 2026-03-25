package com.magiccode.backend.controller;

import com.magiccode.backend.dto.CommentDto;
import com.magiccode.backend.dto.CommentSearchResultDto;
import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.service.CommentService;
import com.magiccode.backend.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostMapping("/admin/post/{postId}")
    public ResponseEntity<CommentDto> addAdminComment(@PathVariable Long postId, @RequestBody CreateCommentRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ROOT"));
        if (!isAdmin) {
            throw new RuntimeException("Access denied");
        }
        CommentDto commentDto = commentService.addComment(postId, request, true);
        return new ResponseEntity<>(commentDto, HttpStatus.CREATED);
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long postId, @RequestBody CreateCommentRequest request) {
        String ip = httpServletRequest.getRemoteAddr();
        if (!rateLimitService.tryAcquire(ip)) {
            throw new RuntimeException("Too many comments, please try again later.");
        }
        CommentDto commentDto = commentService.addComment(postId, request, false); // 始终为 false
        return new ResponseEntity<>(commentDto, HttpStatus.CREATED);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentDto> deleteComment(@PathVariable Long commentId, @RequestParam String email) {
        CommentDto commentDto = commentService.deleteComment(commentId, email);
        return new ResponseEntity<>(commentDto, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROOT')")
    @GetMapping("/search")
    public ResponseEntity<List<CommentSearchResultDto>> searchComments(@RequestParam String q) {
        List<CommentSearchResultDto> results = commentService.searchComments(q);
        return ResponseEntity.ok(results);
    }
}
