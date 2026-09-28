package com.magiccode.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.Comment;
import com.magiccode.backend.model.CommentStatus;
import com.magiccode.backend.model.EmbeddedImage;
import com.magiccode.backend.model.MaintenanceConfig;
import com.magiccode.backend.model.Post;
import com.magiccode.backend.model.User;
import com.magiccode.backend.repository.CategoryRepository;
import com.magiccode.backend.repository.CommentRepository;
import com.magiccode.backend.repository.EmbeddedImageRepository;
import com.magiccode.backend.repository.LikeLogRepository;
import com.magiccode.backend.repository.MaintenanceConfigRepository;
import com.magiccode.backend.repository.PostRepository;
import com.magiccode.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class TestDataSupport {
    static final String ADMIN_USERNAME = "admin";
    static final String ADMIN_PASSWORD = "ChangeMe_StrongPassword_Example";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected PostRepository postRepository;
    @Autowired
    protected CommentRepository commentRepository;
    @Autowired
    protected LikeLogRepository likeLogRepository;
    @Autowired
    protected EmbeddedImageRepository embeddedImageRepository;
    @Autowired
    protected MaintenanceConfigRepository maintenanceConfigRepository;

    protected void clearData() {
        embeddedImageRepository.deleteAll();
        likeLogRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        maintenanceConfigRepository.deleteAll();
    }

    protected User createAdmin() {
        return userRepository.save(User.builder()
                .username(ADMIN_USERNAME)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role("ROOT")
                .build());
    }

    protected String adminToken() throws Exception {
        String body = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("token").asText();
    }

    protected String authHeader() throws Exception {
        return "Bearer " + adminToken();
    }

    protected Category createCategory(String slug) {
        return categoryRepository.save(Category.builder()
                .name("Category " + slug)
                .slug(slug)
                .build());
    }

    protected Post createPost(Category category, String slug, String title, boolean published) {
        return postRepository.save(Post.builder()
                .title(title)
                .slug(slug)
                .content("Body for " + title)
                .category(category)
                .published(published)
                .createdAt(LocalDateTime.now())
                .build());
    }

    protected Comment createComment(Post post, CommentStatus status, String content) {
        return commentRepository.save(Comment.builder()
                .post(post)
                .name("Reader")
                .email("reader@example.com")
                .content(content)
                .status(status)
                .build());
    }

    protected EmbeddedImage createImage(Post post) {
        return embeddedImageRepository.save(EmbeddedImage.builder()
                .ownerType(EmbeddedImage.OwnerType.POST)
                .ownerId(post.getId())
                .originalFilename("image.png")
                .contentType("image/png")
                .size(4L)
                .objectKey("post/" + post.getId() + "/image.png")
                .url("https://cdn.example.test/post/" + post.getId() + "/image.png")
                .build());
    }

    protected MaintenanceConfig createMaintenance(boolean enabled) {
        return maintenanceConfigRepository.save(MaintenanceConfig.builder()
                .enabled(enabled)
                .mode("maintenance")
                .updatedAt(LocalDateTime.now())
                .build());
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
