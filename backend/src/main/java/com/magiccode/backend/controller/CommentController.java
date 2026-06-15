package com.magiccode.backend.controller;

import com.magiccode.backend.config.OpenApiConfig;
import com.magiccode.backend.dto.ApiErrorResponse;
import com.magiccode.backend.dto.AdminCommentPageResponse;
import com.magiccode.backend.dto.BulkCommentRequest;
import com.magiccode.backend.dto.BulkCommentResponse;
import com.magiccode.backend.dto.CommentDto;
import com.magiccode.backend.dto.CommentSearchResultDto;
import com.magiccode.backend.dto.CommentStatsDto;
import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.dto.ValidationErrorResponse;
import com.magiccode.backend.model.CommentStatus;
import com.magiccode.backend.service.CommentService;
import com.magiccode.backend.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@Tag(name = "Public Comments")
public class CommentController {
    private CommentService commentService;
    private final RateLimitService rateLimitService;
    private final HttpServletRequest httpServletRequest;

    @Operation(summary = "List approved post comments", description = "Returns only APPROVED comments for the public post page. PENDING and REJECTED comments are hidden.")
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long postId) {
        List<CommentDto> commentDto = commentService.getCommentsByPostId(postId);
        if (commentDto == null) {
            throw new RuntimeException("Comment Not Found.");
        }
        return new ResponseEntity<>(commentDto, HttpStatus.OK);
    }

    @Operation(summary = "Create admin comment", description = "Creates an admin-authored comment and publishes it as APPROVED.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/admin/post/{postId}")
    public ResponseEntity<CommentDto> addAdminComment(@PathVariable Long postId, @Valid @RequestBody CreateCommentRequest request) {
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

    @Operation(summary = "Submit visitor comment", description = "Creates a visitor comment with status PENDING. It becomes public only after admin approval.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment submitted"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long postId, @Valid @RequestBody CreateCommentRequest request) {
        String ip = httpServletRequest.getRemoteAddr();
        if (!rateLimitService.tryAcquire(ip)) {
            throw new RuntimeException("Too many comments, please try again later.");
        }
        CommentDto commentDto = commentService.addComment(postId, request, false);
        return new ResponseEntity<>(commentDto, HttpStatus.CREATED);
    }

    @Operation(summary = "Deprecated visitor delete endpoint", description = "Visitor self-delete by email is not available. Contact the site administrator.")
    @ApiResponse(responseCode = "400", description = "Visitor deletion is unavailable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @Deprecated
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentDto> deleteComment(@PathVariable Long commentId) {
        throw new RuntimeException("Visitor comment deletion is not available. Please contact the site administrator.");
    }

    @Operation(summary = "Delete comment", description = "Deletes a comment as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/admin/{commentId}")
    public ResponseEntity<CommentDto> deleteAdminComment(@PathVariable Long commentId) {
        CommentDto commentDto = commentService.deleteComment(commentId);
        return new ResponseEntity<>(commentDto, HttpStatus.OK);
    }

    @Operation(summary = "Update comment moderation status", description = "Sets a comment to PENDING, APPROVED, or REJECTED. Only APPROVED comments are public.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/admin/{commentId}/status")
    public ResponseEntity<CommentDto> updateStatus(@PathVariable Long commentId, @RequestParam CommentStatus status) {
        return ResponseEntity.ok(commentService.updateStatus(commentId, status));
    }

    @Operation(summary = "Search comments", description = "Searches comments for admin moderation.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Tag(name = "Admin Comments")
    @PreAuthorize("hasRole('ROOT')")
    @GetMapping("/search")
    public ResponseEntity<List<CommentSearchResultDto>> searchComments(@RequestParam String q) {
        List<CommentSearchResultDto> results = commentService.searchComments(q);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "List comments for moderation", description = "Lists admin comments with optional status, keyword, post, paging, and createdAt sorting filters. Status defaults to PENDING.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Tag(name = "Admin Comments")
    @PreAuthorize("hasRole('ROOT')")
    @GetMapping("/admin")
    public ResponseEntity<AdminCommentPageResponse> listAdminComments(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt desc") String sort) {
        return ResponseEntity.ok(commentService.listAdminComments(status, keyword, postId, page, size, sort));
    }

    @Operation(summary = "Get comment moderation stats", description = "Returns pending, approved, rejected, and total comment counts.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Tag(name = "Admin Comments")
    @PreAuthorize("hasRole('ROOT')")
    @GetMapping("/admin/stats")
    public ResponseEntity<CommentStatsDto> getStats() {
        return ResponseEntity.ok(commentService.getStats());
    }

    @Operation(summary = "Bulk moderate comments", description = "Applies APPROVE, REJECT, or DELETE to selected comments. If any id is missing, the operation fails.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Tag(name = "Admin Comments")
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping("/admin/bulk")
    public ResponseEntity<BulkCommentResponse> bulkAction(@Valid @RequestBody BulkCommentRequest request) {
        return ResponseEntity.ok(commentService.bulkAction(request));
    }
}
