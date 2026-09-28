package com.magiccode.backend;

import com.magiccode.backend.dto.PostDetailDto;
import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

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
class PostApiTests extends TestDataSupport {
    private Category category;

    @BeforeEach
    void setUp() {
        clearData();
        createAdmin();
        category = createCategory("blog");
    }

    @Test
    void publicPostListsHideDraftAndUnpublishedPosts() throws Exception {
        createPost(category, "published", "Published", true);
        createPost(category, "hidden", "Hidden", false);
        createPost(category, "00100000", "Draft", true);

        mockMvc.perform(get("/api/posts/recent").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("published"));

        mockMvc.perform(get("/api/posts/category/{slug}", category.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("published"));
    }

    @Test
    void postDetailReturnsDataAndMissingSlugReturnsNotFoundContract() throws Exception {
        createPost(category, "published", "Published", true);

        mockMvc.perform(get("/api/posts/{slug}", "published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Published"))
                .andExpect(jsonPath("$.categoryName").value(category.getName()));

        mockMvc.perform(get("/api/posts/{slug}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post Not Found"))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void adminPostManagementRequiresAuthAndSupportsCreateUpdateDelete() throws Exception {
        PostDetailDto create = PostDetailDto.builder()
                .title("Admin Post")
                .slug("admin-post")
                .content("created")
                .build();

        mockMvc.perform(post("/api/posts/create")
                        .param("categorySlug", category.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(create)))
                .andExpect(status().isUnauthorized());

        String body = mockMvc.perform(post("/api/posts/create")
                        .header("Authorization", authHeader())
                        .param("categorySlug", category.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("admin-post"))
                .andReturn().getResponse().getContentAsString();

        Long postId = objectMapper.readTree(body).get("id").asLong();
        PostDetailDto update = PostDetailDto.builder()
                .title("Updated Post")
                .slug("updated-post")
                .content("updated")
                .build();

        mockMvc.perform(put("/api/posts/update/{id}", postId)
                        .header("Authorization", authHeader())
                        .param("categorySlug", category.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Post"));

        mockMvc.perform(delete("/api/posts/{slug}", "updated-post")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk());

        assertThat(postRepository.findBySlug("updated-post")).isNull();
    }

    @Test
    void adminPostCreateValidatesRequiredFieldsAndSlugConflicts() throws Exception {
        createPost(category, "existing", "Existing", true);

        mockMvc.perform(post("/api/posts/create")
                        .header("Authorization", authHeader())
                        .param("categorySlug", category.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(PostDetailDto.builder().title("").slug("new").content("body").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Title is Required"));

        mockMvc.perform(post("/api/posts/create")
                        .header("Authorization", authHeader())
                        .param("categorySlug", category.getSlug())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(PostDetailDto.builder().title("Duplicate").slug("existing").content("body").build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Slug already exists, please use another slug."));
    }
}
