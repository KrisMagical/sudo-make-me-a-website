package com.magiccode.backend.service;

import com.magiccode.backend.dto.MovePageRequest;
import com.magiccode.backend.dto.PageDto;
import com.magiccode.backend.mapping.PageMapper;
import com.magiccode.backend.model.Page;
import com.magiccode.backend.model.PageLink;
import com.magiccode.backend.repository.PageLinkRepository;
import com.magiccode.backend.repository.PageRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Data
@Transactional
public class PageService {
    private final PageRepository pageRepository;
    private final PageLinkRepository pageLinkRepository;
    private final PageMapper pageMapper;

    private static final Pattern WIKI_LINK = Pattern.compile("\\\\[\\\\[\\\\s*([a-zA-Z0-9-_./]+)\\\\s*]]");
    private static final Pattern PAGES_PATH = Pattern.compile("/pages/([a-zA-Z0-9-_./]+)");
    private static final String RESERVED_HOME = "home";

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
        rejectReservedSlug(dto.getSlug());
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

        if (dto.getParentId() != null) {
            Page parent = pageRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent Page Not Found"));
            page.setParent(parent);
        } else {
            page.setParent(null);
        }

        if (dto.getOrderIndex() != null) {
            page.setOrderIndex(dto.getOrderIndex());
        }
        pageRepository.save(page);

        syncLinks(page);
        return pageMapper.toDto(page);
    }

    public PageDto updateBySlug(String slug, PageDto dto) {
        rejectReservedSlug(slug);
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

        if (dto.getParentId() != null) {
            Page parent = pageRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent Page Not Found"));
            page.setParent(parent);
        }

        if (dto.getParentId() == null && dto.getOrderIndex() != null) {

        }

        if (dto.getOrderIndex() != null) {
            page.setOrderIndex(dto.getOrderIndex());
        }
        pageRepository.save(page);
        syncLinks(page);
        return pageMapper.toDto(page);
    }

    public void deleteBySlug(String slug) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) {
            throw new RuntimeException("Page Not Found.");
        }
        pageLinkRepository.deleteByFromPage(page);
        pageLinkRepository.deleteByToPage(page);
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
        syncLinks(page);
        return pageMapper.toDto(page);
    }

    public PageDto updateFromMarkdownBySlug(String slug, MultipartFile mdFile) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) throw new RuntimeException("Page Not Found.");
        String md = readAsUtf8(mdFile);
        page.setContent(md);
        pageRepository.save(page);
        syncLinks(page);
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

    public PageDto moveBySlug(String slug, MovePageRequest request) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) {
            throw new RuntimeException("Page Not Found");
        }
        if (request.getParentId() == null) {
            page.setParent(null);
        } else {
            if (Objects.equals(page.getId(), request.getParentId())) {
                throw new RuntimeException("Cannot set parent to itself");
            }
            Page parent = pageRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent Page Not found"));
            if (isDescendant(parent, page)) {
                throw new RuntimeException("Cannot move page under its descendant");
            }
            page.setParent(parent);
        }
        if (request.getOrderIndex() != null) {
            page.setOrderIndex(request.getOrderIndex());
        }
        pageRepository.save(page);
        return pageMapper.toDto(page);
    }

    public List<PageDto> listBackLinks(String slug) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) {
            throw new RuntimeException("Page Not Found");
        }
        return pageLinkRepository.findByToPage(page).stream()
                .map(PageLink::getFromPage)
                .distinct()
                .map(pageMapper::toDto)
                .toList();
    }

    public List<PageDto> listOutlinks(String slug) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) {
            throw new RuntimeException("Page Not Found");
        }

        return pageLinkRepository.findByFromPage(page).stream()
                .map(PageLink::getToPage)
                .distinct()
                .map(pageMapper::toDto)
                .toList();
    }

    private void syncLinks(Page fromPage) {
        String content = fromPage.getContent();
        if (content == null) content = "";

        Set<String> slugs = new HashSet<>();
        slugs.addAll(extractSlugs(WIKI_LINK, content));
        slugs.addAll(extractSlugs(PAGES_PATH, content));

        slugs.remove(fromPage.getSlug());
        if (slugs.isEmpty()) {
            pageLinkRepository.deleteByFromPage(fromPage);
            return;
        }

        List<Page> toPages = pageRepository.findBySlugIn(slugs);
        Map<String, Page> slugToPage = new HashMap<>();
        for (Page p : toPages) {
            slugToPage.put(p.getSlug(), p);
        }

        pageLinkRepository.deleteByFromPage(fromPage);
        for (String toSlug : slugs) {
            Page toPage = slugToPage.get(toSlug);
            if (toPage == null) {
                continue;
            }
            pageLinkRepository.save(PageLink.builder()
                    .fromPage(fromPage)
                    .toPage(toPage)
                    .build());
        }
    }

    private List<String> extractSlugs(Pattern pattern, String content) {
        List<String> out = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String slug = matcher.group(1);
            if (slug != null) {
                slug = slug.trim();
                if (!slug.isBlank()) out.add(slug);
            }
        }
        return out;
    }

    private boolean isDescendant(Page maybeAncestor, Page node) {
        Page current = maybeAncestor;
        while (current != null) {
            if (Objects.equals(current.getId(), node.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void rejectReservedSlug(String slug) {
        if (slug != null && RESERVED_HOME.equalsIgnoreCase(slug.trim())) {
            throw new RuntimeException("Slug 'home' is reserved. Use /api/home instead.");
        }
    }
}