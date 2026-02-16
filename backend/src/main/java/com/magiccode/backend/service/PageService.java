package com.magiccode.backend.service;

import com.magiccode.backend.dto.MovePageRequest;
import com.magiccode.backend.dto.PageDto;
import com.magiccode.backend.mapping.PageMapper;
import com.magiccode.backend.mapping.VideoMapper;
import com.magiccode.backend.model.EmbeddedImage;
import com.magiccode.backend.model.EmbeddedVideo;
import com.magiccode.backend.model.Page;
import com.magiccode.backend.model.PageLink;
import com.magiccode.backend.repository.PageLinkRepository;
import com.magiccode.backend.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PageService {

    private final PageRepository pageRepository;
    private final PageLinkRepository pageLinkRepository;
    private final PageMapper pageMapper;
    private final VideoService videoService;
    private final VideoMapper videoMapper;
    private final ImageService imageService;

    private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[\\s*([a-zA-Z0-9-_./]+)\\s*]]");
    private static final Pattern PAGES_PATH = Pattern.compile("/pages/([a-zA-Z0-9-_./]+)");
    private static final String RESERVED_HOME = "home";

    public PageDto getPageBySlug(String slug) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) {
            throw new RuntimeException("Page Not Found");
        }
        return buildReturnDto(page);
    }

    public List<PageDto> listAll() {
        return pageRepository.findAll()
                .stream()
                .map(this::buildReturnDto)
                .toList();
    }

    public PageDto createPage(PageDto dto) {
        rejectReservedSlug(dto.getSlug());
        validateRequiredFields(dto);

        if (pageRepository.existsBySlug(dto.getSlug())) {
            throw new RuntimeException("Slug already exists");
        }

        Page page = pageMapper.toEntity(dto);

        if (dto.getParentId() != null) {
            Page parent = pageRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent Page Not Found"));
            page.setParent(parent);
        }

        if (dto.getOrderIndex() != null) {
            page.setOrderIndex(dto.getOrderIndex());
        }

        page = pageRepository.save(page);

        syncPageStructure(page);
        videoService.syncFromContent(EmbeddedVideo.OwnerType.PAGE, page.getId(), page.getContent());
        syncLinks(page);

        return buildReturnDto(page);
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

        if (dto.getTitle() != null) page.setTitle(dto.getTitle());
        if (dto.getContent() != null) page.setContent(dto.getContent());

        if (dto.getParentId() != null) {
            if (!Objects.equals(page.getParent() != null ? page.getParent().getId() : null, dto.getParentId())) {
                Page parent = pageRepository.findById(dto.getParentId())
                        .orElseThrow(() -> new RuntimeException("Parent Page Not Found"));
                if (isDescendant(page, parent)) {
                    throw new RuntimeException("Cannot set parent to a descendant of current page");
                }
                page.setParent(parent);
            }
        } else {
            page.setParent(null);
        }

        if (dto.getOrderIndex() != null) {
            page.setOrderIndex(dto.getOrderIndex());
        }

        pageRepository.save(page);

        syncPageStructure(page);
        videoService.syncFromContent(EmbeddedVideo.OwnerType.PAGE, page.getId(), page.getContent());
        syncLinks(page);

        return buildReturnDto(page);
    }

    public void deleteBySlug(String slug) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) {
            throw new RuntimeException("Page Not Found.");
        }
        pageLinkRepository.deleteByFromPage(page);
        pageLinkRepository.deleteByToPage(page);
        imageService.deleteAll(EmbeddedImage.OwnerType.PAGE, page.getId());
        videoService.deleteAll(EmbeddedVideo.OwnerType.PAGE, page.getId());
        pageRepository.delete(page);
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
        return buildReturnDto(page);
    }

    public List<PageDto> listBackLinks(String slug) {
        Page page = pageRepository.findBySlug(slug);
        if (page == null) {
            throw new RuntimeException("Page Not Found");
        }
        return pageLinkRepository.findByToPage(page).stream()
                .map(PageLink::getFromPage)
                .distinct()
                .map(this::buildReturnDto)
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
                .map(this::buildReturnDto)
                .toList();
    }

    private void syncPageStructure(Page currentPage) {
        String content = currentPage.getContent();
        if (content == null || content.isBlank()) return;

        Map<Integer, String> slugPositions = new TreeMap<>();

        Matcher wikiMatcher = WIKI_LINK.matcher(content);
        while (wikiMatcher.find()) {
            String slug = wikiMatcher.group(1);
            if (isValidSlug(slug)) {
                slugPositions.put(wikiMatcher.start(), slug.trim());
            }
        }

        Matcher pathMatcher = PAGES_PATH.matcher(content);
        while (pathMatcher.find()) {
            String slug = pathMatcher.group(1);
            if (isValidSlug(slug)) {
                slugPositions.put(pathMatcher.start(), slug.trim());
            }
        }

        if (slugPositions.isEmpty()) return;

        List<String> slugsInOrder = new ArrayList<>(slugPositions.values());

        List<Page> potentialChildren = pageRepository.findBySlugIn(new HashSet<>(slugsInOrder));
        Map<String, Page> slugMap = potentialChildren.stream()
                .collect(Collectors.toMap(Page::getSlug, p -> p));

        int orderCounter = 0;

        Set<String> processedSlugs = new HashSet<>();

        for (String childSlug : slugsInOrder) {
            if (processedSlugs.contains(childSlug)) continue;
            processedSlugs.add(childSlug);

            Page childPage = slugMap.get(childSlug);

            if (childPage == null) continue;
            if (Objects.equals(childPage.getId(), currentPage.getId())) continue;
            if (isDescendant(childPage, currentPage)) continue;

            boolean changed = false;

            if (childPage.getParent() == null || !childPage.getParent().getId().equals(currentPage.getId())) {
                childPage.setParent(currentPage);
                changed = true;
            }

            if (!Objects.equals(childPage.getOrderIndex(), orderCounter)) {
                childPage.setOrderIndex(orderCounter);
                changed = true;
            }
            if (changed) {
                pageRepository.save(childPage);
            }
            orderCounter++;
        }
    }

    // ==================== 工具方法 ====================
    private boolean isValidSlug(String slug) {
        return slug != null && !slug.isBlank();
    }

    private boolean isDescendant(Page maybeAncestor, Page node) {
        if (maybeAncestor == null || node == null) return false;
        Page current = node.getParent();
        while (current != null) {
            if (Objects.equals(current.getId(), maybeAncestor.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
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
        Map<String, Page> slugToPage = toPages.stream()
                .collect(Collectors.toMap(Page::getSlug, p -> p));

        pageLinkRepository.deleteByFromPage(fromPage);
        for (String toSlug : slugs) {
            Page toPage = slugToPage.get(toSlug);
            if (toPage != null) {
                pageLinkRepository.save(PageLink.builder()
                        .fromPage(fromPage)
                        .toPage(toPage)
                        .build());
            }
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

    private void rejectReservedSlug(String slug) {
        if (slug != null && RESERVED_HOME.equalsIgnoreCase(slug.trim())) {
            throw new RuntimeException("Slug 'home' is reserved. Use /api/home instead.");
        }
    }

    private void validateRequiredFields(PageDto dto) {
        if (dto.getSlug() == null || dto.getSlug().isBlank()) {
            throw new RuntimeException("Slug is Required");
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new RuntimeException("Title is Required");
        }
    }

    private PageDto buildReturnDto(Page page) {
        PageDto result = pageMapper.toDto(page);
        result.setImages(imageService.listPageImages(page.getSlug()));
        result.setVideos(videoMapper.toDtoList(videoService.list(EmbeddedVideo.OwnerType.PAGE, page.getId())));
        return result;
    }
}