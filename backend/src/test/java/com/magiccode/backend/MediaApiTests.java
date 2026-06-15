package com.magiccode.backend;

import com.aliyun.oss.OSS;
import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.EmbeddedImage;
import com.magiccode.backend.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaApiTests extends TestDataSupport {
    @MockitoBean
    private OSS ossClient;

    private Post post;

    @BeforeEach
    void setUp() {
        clearData();
        createAdmin();
        Category category = createCategory("blog");
        post = createPost(category, "media-post", "Media Post", true);
    }

    @Test
    void uploadRequiresAuthAndAcceptsValidImages() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "pixel.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/posts/{postId}/images", post.getId()).file(image))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(multipart("/api/posts/{postId}/images", post.getId())
                        .file(image)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString("https://cdn.example.test/")));

        assertThat(embeddedImageRepository.findAll()).hasSize(1);
    }

    @Test
    void uploadRejectsNonImageEmptyAndMissingPostWithoutLeakingInternals() throws Exception {
        MockMultipartFile text = new MockMultipartFile("file", "note.txt", "text/plain", "not image".getBytes());
        mockMvc.perform(multipart("/api/posts/{postId}/images", post.getId())
                        .file(text)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported image content-type"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/api/posts/{postId}/images", post.getId())
                        .file(empty)
                        .header("Authorization", authHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File is Empty"));

        MockMultipartFile image = new MockMultipartFile("file", "pixel.png", "image/png", new byte[]{1});
        mockMvc.perform(multipart("/api/posts/{postId}/images", 99999L)
                        .file(image)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Post Not Found"));
    }

    @Test
    void mediaListAndDeleteBoundariesAreStable() throws Exception {
        EmbeddedImage image = createImage(post);

        mockMvc.perform(get("/api/posts/{postId}/images", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(image.getId()));

        mockMvc.perform(delete("/api/images/POST/{ownerId}/{imageId}", post.getId(), image.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/images/POST/{ownerId}/{imageId}", post.getId(), image.getId())
                        .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        verify(ossClient).deleteObject(anyString(), anyString());
        assertThat(embeddedImageRepository.findById(image.getId())).isEmpty();

        mockMvc.perform(delete("/api/images/POST/{ownerId}/{imageId}", post.getId(), 99999L)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Image Not Found"));
    }
}
