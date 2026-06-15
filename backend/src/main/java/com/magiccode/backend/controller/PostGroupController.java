package com.magiccode.backend.controller;

import com.magiccode.backend.config.OpenApiConfig;
import com.magiccode.backend.dto.ImageDto;
import com.magiccode.backend.dto.PostGroupDto;
import com.magiccode.backend.dto.PostSummaryDto;
import com.magiccode.backend.service.PostGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collections")
@Tag(name = "Public Posts")
public class PostGroupController {
    private final PostGroupService postGroupService;

    @Operation(summary = "List collections", description = "Returns public post collections.")
    @GetMapping
    public ResponseEntity<List<PostGroupDto>> listAll() {
        return ResponseEntity.ok(postGroupService.listAll());
    }

    @Operation(summary = "Get collection", description = "Returns a public collection by slug.")
    @GetMapping("/{slug}")
    public ResponseEntity<PostGroupDto> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(postGroupService.getBySlug(slug));
    }

    @Operation(summary = "Create collection", description = "Creates a post collection as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping
    public ResponseEntity<PostGroupDto> create(@RequestBody PostGroupDto dto) {
        return new ResponseEntity<>(postGroupService.create(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Update collection", description = "Updates a post collection as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/{id}")
    public ResponseEntity<PostGroupDto> update(@PathVariable Long id, @RequestBody PostGroupDto dto) {
        return ResponseEntity.ok(postGroupService.update(id, dto));
    }

    @Operation(summary = "Delete collection", description = "Deletes a post collection as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean deletePosts) {
        postGroupService.delete(id, deletePosts);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add post to collection", description = "Adds a post to a collection as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping("/{postGroupId}/posts/{postId}")
    public ResponseEntity<Void> addPost(
            @PathVariable Long postGroupId,
            @PathVariable Long postId,
            @RequestParam(required = false) Integer orderIndex) {
        postGroupService.addPost(postGroupId, postId, orderIndex);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove post from collection", description = "Removes a post from a collection as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/{postGroupId}/posts/{postId}")
    public ResponseEntity<Void> removePost(
            @PathVariable Long postGroupId,
            @PathVariable Long postId) {
        postGroupService.removePost(postGroupId, postId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reorder collection posts", description = "Reorders posts in a collection as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/{postGroupId}/posts/reorder")
    public ResponseEntity<Void> reorderPosts(
            @PathVariable Long postGroupId,
            @RequestBody List<Long> orderedPostIds) {
        postGroupService.reorderPosts(postGroupId, orderedPostIds);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Upload collection cover", description = "Uploads a collection cover image as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/{postGroupId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageDto> uploadCover(
            @PathVariable Long postGroupId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(postGroupService.uploadCoverImage(postGroupId, file));
    }

    @Operation(summary = "Search collections", description = "Searches public collections by keyword.")
    @Tag(name = "Public Search")
    @GetMapping("/search")
    public ResponseEntity<List<PostGroupDto>> search(@RequestParam String q) {
        return ResponseEntity.ok(postGroupService.search(q));
    }

    @Operation(summary = "Search posts in collection", description = "Searches public posts within a collection.")
    @Tag(name = "Public Search")
    @GetMapping("/{slug}/search")
    public ResponseEntity<Page<PostSummaryDto>> searchPostsInCollection(
            @PathVariable String slug,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PostGroupDto group = postGroupService.getBySlug(slug);
        if (group == null) {
            throw new RuntimeException("Collection not found");
        }
        Page<PostSummaryDto> result = postGroupService.searchPostsInCollection(group.getId(), q, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "List posts in collection", description = "Returns paged public posts in a collection.")
    @GetMapping("/{slug}/posts")
    public ResponseEntity<Page<PostSummaryDto>> getPostsInCollection(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PostGroupDto group = postGroupService.getBySlug(slug);
        Page<PostSummaryDto> result = postGroupService.getPostsByCollection(group.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }
}
