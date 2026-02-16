package com.magiccode.backend.controller;

import com.magiccode.backend.dto.LikeResponseDto;
import com.magiccode.backend.dto.PostDetailDto;
import com.magiccode.backend.dto.PostSummaryDto;
import com.magiccode.backend.service.LikeLogService;
import com.magiccode.backend.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final LikeLogService likeLogService;

    @GetMapping("/category/{slug}")
    public ResponseEntity<List<PostSummaryDto>> getPostByCategory(@PathVariable String slug) {
        List<PostSummaryDto> postSummaryDto = postService.getPostByCategorySlug(slug);
        if (postSummaryDto == null) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(postSummaryDto, HttpStatus.OK);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PostDetailDto> getPostDetail(@PathVariable String slug) throws RuntimeException {
        PostDetailDto postDetailDto = postService.getPostBySlug(slug);
        if (postDetailDto == null) {
            throw new RuntimeException("Slug Not Found.");
        }
        return new ResponseEntity<>(postDetailDto, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROOT')")
    @PostMapping("/create")
    public ResponseEntity<PostDetailDto> createPost(@RequestParam String categorySlug, @RequestBody PostDetailDto postDetailDto) {
        PostDetailDto postDetailDto_create = postService.createPost(postDetailDto, categorySlug);
        if (postDetailDto_create == null) {
            throw new RuntimeException("Create Failed");
        }
        return new ResponseEntity<>(postDetailDto_create, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/update/{id}")
    public ResponseEntity<PostDetailDto> updatePost(@PathVariable Long id, @RequestParam(required = false) String categorySlug, @RequestBody PostDetailDto postDetailDto) {
        PostDetailDto updatedPost = postService.updatePost(id, postDetailDto, categorySlug);
        return new ResponseEntity<>(updatedPost, HttpStatus.OK);
    }


    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/{slug}")
    public ResponseEntity<String> deletePostBySlug(@PathVariable String slug) {
        postService.deletePostBySlug(slug);
        return ResponseEntity.ok(slug);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<PostSummaryDto>> getRecentPosts(@RequestParam(defaultValue = "5") int limit) {
        List<PostSummaryDto> dto = postService.getRecentPosts(limit);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeResponseDto> likePost(@PathVariable Long postId, @RequestParam boolean positive, HttpServletRequest request) {
        String identifier = request.getRemoteAddr(); // 获取客户端 IP 作为 identifier
        try {
            likeLogService.addLikeOrDislike(postId, identifier, positive);
            LikeResponseDto response = new LikeResponseDto(
                    likeLogService.countLikesByPostId(postId),
                    likeLogService.countDisLikesByPostId(postId)
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new LikeResponseDto(0, 0, e.getMessage()), HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/{slug}/likes")
    public ResponseEntity<LikeResponseDto> getLikeAndDislikeCount(@PathVariable String slug) {
        try {
            LikeResponseDto response = likeLogService.getLikeAndDislikeCountBySlug(slug);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new LikeResponseDto(0, 0, e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

}
