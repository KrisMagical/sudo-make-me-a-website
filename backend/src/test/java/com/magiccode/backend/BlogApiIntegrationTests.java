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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
}
