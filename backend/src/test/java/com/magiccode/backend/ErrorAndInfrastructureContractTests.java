package com.magiccode.backend;

import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.Comment;
import com.magiccode.backend.model.CommentStatus;
import com.magiccode.backend.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorAndInfrastructureContractTests extends TestDataSupport {
    private Post post;

    @BeforeEach
    void setUp() {
        clearData();
        createAdmin();
        Category category = createCategory("blog");
        post = createPost(category, "contract-post", "Contract Post", true);
    }

    @Test
    void validationUnauthorizedNotFoundAndConflictUseMessageContract() throws Exception {
        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("", "bad-email", " ", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.error").doesNotExist());

        mockMvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"))
                .andExpect(jsonPath("$.error").doesNotExist());

        mockMvc.perform(get("/api/posts/missing-slug"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post Not Found"))
                .andExpect(jsonPath("$.error").doesNotExist());

        mockMvc.perform(post("/api/posts/{postId}/like", post.getId()).param("positive", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/posts/{postId}/like", post.getId()).param("positive", "true"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already liked")));
    }

    @Test
    void commentAndLikeRegressionBoundariesHold() throws Exception {
        Comment approved = createComment(post, CommentStatus.APPROVED, "Approved first");
        createComment(post, CommentStatus.PENDING, "Pending hidden");
        createComment(post, CommentStatus.REJECTED, "Rejected hidden");

        mockMvc.perform(get("/api/comments/post/{postId}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Approved first"))
                .andExpect(jsonPath("$[0].moderationReason").doesNotExist());

        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .with(request -> {
                            request.setRemoteAddr("10.2.0.1");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Reader", "reader@example.com", "Reply", 99999L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parent comment not found"));

        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .with(request -> {
                            request.setRemoteAddr("10.2.0.2");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Reader", "reader@example.com", "Reply", approved.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/comments/admin/bulk")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("commentIds", List.of(approved.getId(), 99999L), "action", "APPROVE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Comments not found")));

        mockMvc.perform(post("/api/posts/{postId}/like", 99999L).param("positive", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post Not Found"));

        mockMvc.perform(post("/api/posts/{postId}/like", post.getId())
                        .with(request -> {
                            request.setRemoteAddr("10.1.1.1");
                            return request;
                        })
                        .param("positive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.dislikes").value(1));

        mockMvc.perform(post("/api/posts/{postId}/like", post.getId())
                        .with(request -> {
                            request.setRemoteAddr("10.1.1.1");
                            return request;
                        })
                        .param("positive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1))
                .andExpect(jsonPath("$.dislikes").value(0));

        assertThat(likeLogRepository.findAll()).hasSize(1);
    }

    @Test
    void actuatorOpenApiAndRequestIdContractsRemainStable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/api/posts/recent").header("X-Request-Id", "contract-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "contract-request-id"));

        mockMvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"));
    }
}
