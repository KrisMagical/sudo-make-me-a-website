package com.magiccode.backend.service;

import com.magiccode.backend.dto.PostDetailDto;
import com.magiccode.backend.dto.PostSummaryDto;
import com.magiccode.backend.mapping.PostDetailMapper;
import com.magiccode.backend.mapping.PostSummaryMapper;
import com.magiccode.backend.model.Category;
import com.magiccode.backend.model.Post;
import com.magiccode.backend.repository.CategoryRepository;
import com.magiccode.backend.repository.PostRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Data
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final PostSummaryMapper postSummaryMapper;
    private final PostDetailMapper postDetailMapper;
    private final LikeLogService likeLogService;
    @Value("${upload.image.path}")
    private String imageUploadPath;

    @Value("${upload.video.path}")
    private String videoUploadPath;

    public List<PostSummaryDto> getPostByCategorySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug);
        if (category != null) {
            return postSummaryMapper.toPostSummaryDtoList(postRepository.findByCategory(category));
        } else {
            throw new RuntimeException("Post Not Found.");
        }
    }

    public PostDetailDto getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug);
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            return postDetailMapper.toPostDetailDto(post);
        } else {
            throw new RuntimeException("Post Not Found.");
        }
    }

    public PostDetailDto createPost(PostDetailDto postDetailDto, String categorySlug) {
        Category category = categoryRepository.findBySlug(categorySlug);
        if (category == null) {
            throw new RuntimeException("Category Not Found.");
        }
        if (postDetailDto.getSlug() == null || postDetailDto.getSlug().isBlank()) {
            throw new RuntimeException("Slug is Required");
        }
        if (postDetailDto.getTitle() == null || postDetailDto.getTitle().isBlank()) {
            throw new RuntimeException("Title is Required");
        }
        Post post = postDetailMapper.toPostEntity(postDetailDto);
        post.setCategory(category);
        postRepository.save(post);
        return postDetailMapper.toPostDetailDto(post);
    }

    public PostDetailDto createPostFromMarkdown(String categorySlug, MultipartFile mdFile, String slug, String title) {
        String mdContent = readMultipartAsUtf8(mdFile);
        if (slug == null || slug.isBlank()) {
            throw new RuntimeException("Slug is Required");
        }
        ensureSlugUnique(slug);
        String finalTitle = (title != null && !title.isBlank()) ? title : (extractTitleFromMarkdown(mdContent) != null ? extractTitleFromMarkdown(mdContent) : slug);


        PostDetailDto postDetailDto = new PostDetailDto();
        postDetailDto.setSlug(slug);
        postDetailDto.setTitle(finalTitle);
        postDetailDto.setContent(mdContent);
        return createPost(postDetailDto, categorySlug);
    }

    public PostDetailDto updatePost(Long id, PostDetailDto updatePostDetailDto, String categorySlug) {
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post Not Found"));
        if (existingPost == null) {
            throw new RuntimeException("Post Not Found.");
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug);
            if (category == null) {
                throw new RuntimeException("Category Not Found");
            }
            existingPost.setCategory(category);
        }
        if (updatePostDetailDto.getTitle() != null) {
            existingPost.setTitle(updatePostDetailDto.getTitle());
        }
        if (updatePostDetailDto.getSlug() != null) {
            existingPost.setSlug(updatePostDetailDto.getSlug());
        }
        if (updatePostDetailDto.getContent() != null) {
            existingPost.setContent(updatePostDetailDto.getContent());
        }
        postRepository.save(existingPost);
        return postDetailMapper.toPostDetailDto(existingPost);
    }

    public PostDetailDto updatePostFromMarkDown(Long id, MultipartFile mdFile, String categorySlug) {
        String mdContext = readMultipartAsUtf8(mdFile);

        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post Not Found"));
        if (existingPost == null) {
            throw new RuntimeException("Post Not Found");
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug);
            if (category == null) {
                throw new RuntimeException("category Not Found");
            }
            existingPost.setCategory(category);
        }
        existingPost.setContent(mdContext);
        postRepository.save(existingPost);
        return postDetailMapper.toPostDetailDto(existingPost);
    }

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is Empty");
        }
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("image");
        String safe = sanitizeFilenameKeepExt(original);
        String filename = UUID.randomUUID() + "_" + safe;

        Path filepath = Paths.get(imageUploadPath, filename);
        try {
            Files.createDirectories(filepath.getParent());
            Files.write(filepath, getFileBytes(file));
        } catch (IOException e) {
            throw new RuntimeException("Upload Failed");
        }
        String encoded = UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);
        return "/images/" + encoded;
        /*
            图片保存路径
            upload.image.path=/var/www/blog/image → /images/{filename}s
        */
    }

    public String uploadVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is Empty");
        }
        String original = file.getOriginalFilename();
        String ext = extractExtensionOrEmpty(original);
        Set<String> allowed = Set.of("mp4", "webm", "ogg");
        if (!allowed.contains(ext.toLowerCase())) {
            throw new RuntimeException("Unsupported video format");
        }

        String safe = sanitizeFilenameKeepExt(original);
        if (!safe.toLowerCase(Locale.ROOT).endsWith("." + ext)) {
            safe = safe + "." + ext;
        }
        String filename = UUID.randomUUID() + "_" + safe;
        Path filepath = Paths.get(videoUploadPath, "Videos", filename);
        try {
            Files.createDirectories(filepath.getParent());
            Files.write(filepath, getFileBytes(file));
        } catch (IOException e) {
            throw new RuntimeException("Upload Failed");
        }
        String encoded = UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);
        return "/videos/" + encoded;
        /*
        Markdown 文件上传时
        因为/create-md 和 /update-md 已经把 Markdown 内容读进数据库了，所以只要 Markdown 文件里包含视频 URL，比如：
        @[video](/videos/test.mp4)
        @[video](https://www.youtube.com/embed/abc123)
        前端渲染时就能识别。后端这里 不需要特殊解析，因为 content 就是 Markdown 原文，交由前端去渲染 <video> 或 <iframe>。
        视频文件夹：/var/www/blog/videos → /videos/{filename}
     */
    }

    public void deletePostBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new RuntimeException("Slug is Required");
        }
        Post post = postRepository.findBySlug(slug);
        if (post == null) {
            throw new RuntimeException("Post Not Found");
        }
        likeLogService.deleteAllByPostId(post.getId());
        postRepository.delete(post);
    }

    //Tools Methods
    private static final Pattern UNSAFE_CHARS =
            Pattern.compile("[\\\\/:*?\"<>|#%&{}$!@`^~;+=]");

    private String sanitizeFilenameKeepExt(String original) {
        String name = original.trim();

        // 分离扩展名（最后一个点）
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            ext = name.substring(dot);           // 包含 '.'
            name = name.substring(0, dot);
        }

        // 空白 → 下划线；去除不安全字符
        name = name.replaceAll("\\s+", "_");
        name = UNSAFE_CHARS.matcher(name).replaceAll("_");

        // 避免全空
        if (name.isBlank()) name = "file";

        // 控制主体长度，给 UUID 和扩展名留足空间（总长 < 200 较稳妥）
        if (name.length() > 160) {
            name = name.substring(0, 160);
        }

        // 扩展名也防御性裁剪（很少见）
        if (ext.length() > 16) {
            ext = ext.substring(0, 16);
        }

        return name + ext;
    }

    private String readMultipartAsUtf8(MultipartFile file) {
        try {
            return new String(getFileBytes(file), StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private byte[] getFileBytes(MultipartFile file) {
        if (file == null) throw new RuntimeException("File is empty");
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Read file failed");
        }
    }

    private String extractExtensionOrEmpty(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1);
    }

    private void ensureSlugUnique(String slug) {
        if (postRepository.findBySlug(slug) != null) {
            throw new RuntimeException("Slug already exists");
        }
    }

    /**
     * 从 Markdown 文本中提取第一个一级标题(# )作为标题
     */
    private String extractTitleFromMarkdown(String md) {
        if (md == null) return null;
        // 常见形式：以 "# " 开头的一行
        String[] lines = md.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
            // 兼容 "#\t"、" #  " 等
            if (trimmed.startsWith("#")) {
                String after = trimmed.substring(1).trim();
                if (!after.isBlank()) return after;
            }
        }
        return null;
    }
}