package com.magiccode.backend.controller;

import com.magiccode.backend.dto.LikeResponseDto;
import com.magiccode.backend.dto.PostDetailDto;
import com.magiccode.backend.dto.PostSummaryDto;
import com.magiccode.backend.config.OpenApiConfig;
import com.magiccode.backend.dto.ApiErrorResponse;
import com.magiccode.backend.service.LikeLogService;
import com.magiccode.backend.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Tag(name = "Public Posts")
public class PostController {
    private final PostService postService;
    private final LikeLogService likeLogService;

    @Operation(summary = "List posts by category", description = "Returns public post summaries for a category slug.")
    @GetMapping("/category/{slug}")
    public ResponseEntity<Page<PostSummaryDto>> getPostByCategory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostSummaryDto> postSummaryPage = postService.getPostByCategorySlug(slug, PageRequest.of(page, size));
        return ResponseEntity.ok(postSummaryPage);
    }

    @Operation(summary = "Get post detail", description = "Returns a public post detail by slug, including approved comments and media.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post found"),
            @ApiResponse(responseCode = "400", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{slug}")
    public ResponseEntity<PostDetailDto> getPostDetail(@PathVariable String slug) throws RuntimeException {
        PostDetailDto postDetailDto = postService.getPostBySlug(slug);
        if (postDetailDto == null) {
            throw new RuntimeException("Slug Not Found.");
        }
        return new ResponseEntity<>(postDetailDto, HttpStatus.OK);
    }

    @Operation(summary = "Create post", description = "Creates a post in the selected category.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping("/create")
    public ResponseEntity<PostDetailDto> createPost(@RequestParam String categorySlug, @RequestBody PostDetailDto postDetailDto) {
        PostDetailDto postDetailDto_create = postService.createPost(postDetailDto, categorySlug);
        if (postDetailDto_create == null) {
            throw new RuntimeException("Create Failed");
        }
        return new ResponseEntity<>(postDetailDto_create, HttpStatus.CREATED);
    }

    @Operation(summary = "Update post", description = "Updates an existing post.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/update/{id}")
    public ResponseEntity<PostDetailDto> updatePost(@PathVariable Long id, @RequestParam(required = false) String categorySlug, @RequestBody PostDetailDto postDetailDto) {
        PostDetailDto updatedPost = postService.updatePost(id, postDetailDto, categorySlug);
        return new ResponseEntity<>(updatedPost, HttpStatus.OK);
    }


    @Operation(summary = "Delete post", description = "Deletes a post by slug.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/{slug}")
    public ResponseEntity<String> deletePostBySlug(@PathVariable String slug) {
        postService.deletePostBySlug(slug);
        return ResponseEntity.ok(slug);
    }

    @Operation(summary = "List recent posts", description = "Returns the most recent public post summaries.")
    @GetMapping("/recent")
    public ResponseEntity<List<PostSummaryDto>> getRecentPosts(@RequestParam(defaultValue = "6") int limit) {
        List<PostSummaryDto> dto = postService.getRecentPosts(limit);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "React to post", description = "Adds a like or dislike for the client identifier. Repeating the same reaction returns 409 and does not increment counts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reaction recorded or switched"),
            @ApiResponse(responseCode = "409", description = "Duplicate reaction",
                    content = @Content(schema = @Schema(implementation = LikeResponseDto.class)))
    })
    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeResponseDto> likePost(@PathVariable Long postId, @RequestParam boolean positive, HttpServletRequest request) {
        String identifier = request.getRemoteAddr();
        try {
            likeLogService.addLikeOrDislike(postId, identifier, positive);
            LikeResponseDto response = new LikeResponseDto(
                    likeLogService.countLikesByPostId(postId),
                    likeLogService.countDisLikesByPostId(postId)
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            if (e instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            return new ResponseEntity<>(new LikeResponseDto(0, 0, e.getMessage()), HttpStatus.CONFLICT);
        }
    }

    @Operation(summary = "Get reaction counts", description = "Returns current like and dislike counts for a post slug.")
    @GetMapping("/{slug}/likes")
    public ResponseEntity<LikeResponseDto> getLikeAndDislikeCount(@PathVariable String slug) {
        try {
            LikeResponseDto response = likeLogService.getLikeAndDislikeCountBySlug(slug);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            if (e instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            return new ResponseEntity<>(new LikeResponseDto(0, 0, e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Search posts", description = "Searches public posts by keyword.")
    @Tag(name = "Public Search")
    @GetMapping("/searchPages")
    public ResponseEntity<List<PostSummaryDto>> searchPosts(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        List<PostSummaryDto> results = postService.searchPosts(q, limit);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Search posts in category", description = "Searches public posts within a category.")
    @Tag(name = "Public Search")
    @GetMapping("/category/{slug}/search")
    public ResponseEntity<Page<PostSummaryDto>> searchPostsByCategory(
            @PathVariable String slug,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostSummaryDto> result = postService.searchPostsByCategorySlug(slug, q, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }
}
