package com.magiccode.backend.controller;

import com.magiccode.backend.dto.LikeResponseDto;
import com.magiccode.backend.dto.PostDetailDto;
import com.magiccode.backend.dto.PostSummaryDto;
import com.magiccode.backend.service.LikeLogService;
import com.magiccode.backend.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PostMapping(value = "/create-md", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<PostDetailDto> createPostFromMarkdown(@RequestParam String categorySlug, @RequestParam("file") MultipartFile mdFile,@RequestParam String slug,@RequestParam(required = false) String title) {
        return new ResponseEntity<>(postService.createPostFromMarkdown(categorySlug, mdFile,slug,title), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/update/{id}")
    public ResponseEntity<PostDetailDto> updatePost(@PathVariable Long id, @RequestParam(required = false) String categorySlug, @RequestBody PostDetailDto postDetailDto) {
        PostDetailDto updatedPost = postService.updatePost(id, postDetailDto, categorySlug);
        return new ResponseEntity<>(updatedPost, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROOT')")
    @PutMapping(value = "/update-md/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<PostDetailDto> updatePostFromMarkdown(@PathVariable Long id, @RequestParam(required = false) String categorySlug, @RequestParam("file") MultipartFile mdFile) {
        return new ResponseEntity<>(postService.updatePostFromMarkDown(id, mdFile, categorySlug), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/upload/image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = postService.uploadImage(file);
            return ResponseEntity.ok(fileUrl);
        } catch (RuntimeException e) {
            if (e.getMessage().equals("File is Empty")) {
                return new ResponseEntity<>("File is Empty", HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
        }
    }


    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/upload/video", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = postService.uploadVideo(file);
            return ResponseEntity.ok(fileUrl);
        } catch (RuntimeException e) {
            if (e.getMessage().equals("File is Empty")) {
                return new ResponseEntity<>("File is Empty", HttpStatus.BAD_REQUEST);
            } else if (e.getMessage().equals("Unsupported video format")) {
                return new ResponseEntity<>("Unsupported video format", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload Failed");
        }
    }

    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/{slug}")
    public ResponseEntity<String> deletePostBySlug(@PathVariable String slug){
        postService.deletePostBySlug(slug);
        return ResponseEntity.ok(slug);
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
