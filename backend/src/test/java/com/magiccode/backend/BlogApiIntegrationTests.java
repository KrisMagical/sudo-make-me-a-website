package com.magiccode.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiccode.backend.dto.CreateCommentRequest;
import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.Comment;
import com.magiccode.backend.model.CommentStatus;
import com.magiccode.backend.model.Post;
import com.magiccode.backend.model.User;
import com.magiccode.backend.repository.CategoryRepository;
import com.magiccode.backend.repository.CommentRepository;
import com.magiccode.backend.repository.LikeLogRepository;
import com.magiccode.backend.repository.PostRepository;
import com.magiccode.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlogApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private LikeLogRepository likeLogRepository;

    private Post post;

    @BeforeEach
    void setUp() {
        likeLogRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("StrongPass123!"))
                .role("ROOT")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .name("Blog")
                .slug("blog")
                .build());

        post = postRepository.save(Post.builder()
                .title("Hello")
                .slug("hello")
                .content("content")
                .category(category)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    void invalidCommentRequestsReturnValidationErrors() throws Exception {
        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("", "reader@example.com", "ok", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists());

        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Reader", "bad-email", "ok", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Reader", "reader@example.com", " ", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.content").exists());

        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Reader", "reader@example.com", "x".repeat(2001), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.content").exists());
    }

    @Test
    void visitorCommentIsPendingAndHiddenUntilApproved() throws Exception {
        String body = mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Reader", "reader@example.com", "Waiting", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        Long commentId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/comments/post/{postId}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(put("/api/comments/admin/{commentId}/status", commentId)
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/comments/post/{postId}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Waiting"));
    }

    @Test
    void adminDeleteRequiresAuthenticationAndThenDeletes() throws Exception {
        Comment comment = commentRepository.save(Comment.builder()
                .post(post)
                .name("Reader")
                .email("reader@example.com")
                .content("Remove me")
                .status(CommentStatus.APPROVED)
                .build());

        mockMvc.perform(delete("/api/comments/admin/{commentId}", comment.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/comments/admin/{commentId}", comment.getId())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        assertThat(commentRepository.findById(comment.getId())).isEmpty();
    }

    @Test
    void adminCommentListFiltersSearchesAndPaginates() throws Exception {
        saveComment("Pending Reader", "pending@example.com", "Need review", CommentStatus.PENDING);
        saveComment("Approved Reader", "approved@example.com", "Visible comment", CommentStatus.APPROVED);
        saveComment("Rejected Reader", "rejected@example.com", "Spam phrase", CommentStatus.REJECTED);

        mockMvc.perform(get("/api/comments/admin")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].status").value("PENDING"));

        mockMvc.perform(get("/api/comments/admin")
                        .param("status", "APPROVED")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"));

        mockMvc.perform(get("/api/comments/admin")
                        .param("status", "ALL")
                        .param("keyword", "spam")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].email").value("rejected@example.com"));

        mockMvc.perform(get("/api/comments/admin")
                        .param("status", "ALL")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void commentStatsRequireAuthenticationAndReturnCounts() throws Exception {
        saveComment("Pending Reader", "pending@example.com", "Need review", CommentStatus.PENDING);
        saveComment("Approved Reader", "approved@example.com", "Visible comment", CommentStatus.APPROVED);
        saveComment("Rejected Reader", "rejected@example.com", "Spam phrase", CommentStatus.REJECTED);

        mockMvc.perform(get("/api/comments/admin/stats"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/comments/admin/stats")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.approved").value(1))
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void bulkCommentActionsRequireAuthenticationAndValidateInput() throws Exception {
        Comment first = saveComment("First", "first@example.com", "First pending", CommentStatus.PENDING);
        Comment second = saveComment("Second", "second@example.com", "Second pending", CommentStatus.PENDING);

        mockMvc.perform(post("/api/comments/admin/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("commentIds", List.of(first.getId()), "action", "APPROVE"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/comments/admin/bulk")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("commentIds", List.of(), "action", "APPROVE"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/comments/admin/bulk")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("commentIds", List.of(first.getId(), second.getId()), "action", "APPROVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affected").value(2));

        assertThat(commentRepository.findAll().stream().map(Comment::getStatus)).containsOnly(CommentStatus.APPROVED);

        mockMvc.perform(post("/api/comments/admin/bulk")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("commentIds", List.of(first.getId(), second.getId()), "action", "REJECT"))))
                .andExpect(status().isOk());
        assertThat(commentRepository.findAll().stream().map(Comment::getStatus)).containsOnly(CommentStatus.REJECTED);

        mockMvc.perform(post("/api/comments/admin/bulk")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("commentIds", List.of(first.getId(), second.getId()), "action", "DELETE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affected").value(2));
        assertThat(commentRepository.findAll()).isEmpty();
    }

    @Test
    void moderationRulesRejectObviousSpamAndKeepPublicResponseClean() throws Exception {
        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.1");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Reader", "reader@example.com", "https://one.test https://two.test https://three.test", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.moderationReason").doesNotExist());

        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.2");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Reader", "reader@example.com", "This has blockedword inside", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/comments/post/{postId}", post.getId())
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.3");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CreateCommentRequest("Reader", "reader@example.com", "A normal thoughtful comment", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/comments/admin")
                        .param("status", "REJECTED")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].moderationReason").exists());
    }

    @Test
    void duplicateLikeDoesNotIncreaseCountAndSwitchingReactionUpdatesCounts() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/like", post.getId()).param("positive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1))
                .andExpect(jsonPath("$.dislikes").value(0));

        mockMvc.perform(post("/api/posts/{postId}/like", post.getId()).param("positive", "true"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/posts/{postId}/like", post.getId()).param("positive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.dislikes").value(1));

        assertThat(likeLogRepository.findAll()).hasSize(1);
    }

    @Test
    void loginSuccessAndFailure() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "StrongPass123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void actuatorHealthAndInfoAreAvailableInTestProfile() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("sudo-make-me-a-website"));
    }

    @Test
    void requestIdHeaderIsReturnedAndReused() throws Exception {
        mockMvc.perform(get("/api/posts/recent"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(get("/api/posts/recent")
                        .header("X-Request-Id", "test-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-id"));

        mockMvc.perform(get("/api/comments/admin/stats"))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void openApiDocsAreAvailableInTestProfile() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("sudo-make-me-a-website API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/admin/auth/me'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/posts/recent'].get.security").doesNotExist());
    }

    private String adminToken() throws Exception {
        String loginBody = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "StrongPass123!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(loginBody);
        return node.get("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private Comment saveComment(String name, String email, String content, CommentStatus status) {
        return commentRepository.save(Comment.builder()
                .post(post)
                .name(name)
                .email(email)
                .content(content)
                .status(status)
                .build());
    }
}
