package com.magiccode.backend.service;

import com.magiccode.backend.dto.PageDto;
import com.magiccode.backend.mapping.PageMapper;
import com.magiccode.backend.model.Page;
import com.magiccode.backend.repository.PageRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Data
@Transactional
public class PageService {
    private final PageRepository pageRepository;
    private final PageMapper pageMapper;

    public PageDto getPageBySlug(String slug) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) {
            throw new RuntimeException("Page Not Found");
        }
        return pageMapper.toDto(page);
    }
    public List<PageDto> listAll() {
        return pageRepository.findAll()
                .stream()
                .map(pageMapper::toDto)
                .toList();
    }

    public PageDto create(PageDto dto) {
        if (dto.getSlug() == null || dto.getSlug().isBlank()) {
            throw new RuntimeException("Slug is Required");
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new RuntimeException("Title is Required");
        }
        if (pageRepository.existsBySlug(dto.getSlug())) {
            throw new RuntimeException("Slug already exists");
        }
        Page page = pageMapper.toEntity(dto);
        pageRepository.save(page);
        return pageMapper.toDto(page);
    }
    public PageDto updateBySlug(String slug, PageDto dto) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) throw new RuntimeException("Page Not Found.");
        if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
            String newSlug = dto.getSlug().trim();
            if (!newSlug.equals(page.getSlug()) && pageRepository.existsBySlug(newSlug)) {
                throw new RuntimeException("Slug already exists");
            }
            page.setSlug(newSlug);
        }

        if (dto.getTitle() != null) {
            page.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            page.setContent(dto.getContent());
        }

        pageRepository.save(page);
        return pageMapper.toDto(page);
    }

    public void deleteBySlug(String slug) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) {
            throw new RuntimeException("Page Not Found.");
        }
        pageRepository.delete(page);
    }
    public PageDto createFromMarkdown(MultipartFile mdFile, String slug, String title) {
        String md = readAsUtf8(mdFile);
        if (slug == null || slug.isBlank()) throw new RuntimeException("Slug is Required");
        if (pageRepository.existsBySlug(slug)) throw new RuntimeException("Slug already exists");
        String finalTitle = (title != null && !title.isBlank())
                ? title
                : (extractTitleFromMarkdown(md) != null ? extractTitleFromMarkdown(md) : slug);

        Page page = Page.builder()
                .slug(slug)
                .title(finalTitle)
                .content(md)
                .build();
        pageRepository.save(page);
        return pageMapper.toDto(page);
    }
    public PageDto updateFromMarkdownBySlug(String slug, MultipartFile mdFile) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) throw new RuntimeException("Page Not Found.");
        String md = readAsUtf8(mdFile);
        page.setContent(md);
        pageRepository.save(page);
        return pageMapper.toDto(page);
    }
    private String readAsUtf8(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) throw new RuntimeException("File is Empty");
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Read file failed");
        }
    }

    // 复用你在 PostService 里的 Markdown 标题提取逻辑
    private String extractTitleFromMarkdown(String md) {
        if (md == null) return null;
        String[] lines = md.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) return trimmed.substring(2).trim();
            if (trimmed.startsWith("#")) {
                String after = trimmed.substring(1).trim();
                if (!after.isBlank()) return after;
            }
        }
        return null;
    }
}
