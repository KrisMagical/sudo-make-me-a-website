package com.magiccode.backend.controller;

import com.magiccode.backend.dto.PageDto;
import com.magiccode.backend.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pages")
public class PageController {
    private final PageService pageService;

    @GetMapping("/{slug}")
    public ResponseEntity<PageDto> getPageBySlug(@PathVariable String slug) {
        PageDto pageDto=pageService.getPageBySlug(slug);
        return new ResponseEntity<>(pageDto, HttpStatus.OK);
    }
    @GetMapping
    public ResponseEntity<List<PageDto>> listPages() {
        return ResponseEntity.ok(pageService.listAll());
    }
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping
    public ResponseEntity<PageDto> createPage(@RequestBody PageDto dto) {
        PageDto created = pageService.create(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }


    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/create-md", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<PageDto> createPageFromMarkdown(
            @RequestParam("file") MultipartFile mdFile,
            @RequestParam String slug,
            @RequestParam(required = false) String title
    ) {
        PageDto created = pageService.createFromMarkdown(mdFile, slug, title);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }


    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/{slug}")
    public ResponseEntity<PageDto> updatePageBySlug(@PathVariable String slug, @RequestBody PageDto dto) {
        PageDto updated = pageService.updateBySlug(slug, dto);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }


    @PreAuthorize("hasRole('ROOT')")
    @PutMapping(value = "/update-md/{slug}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<PageDto> updatePageFromMarkdownBySlug(
            @PathVariable String slug,
            @RequestParam("file") MultipartFile mdFile
    ) {
        PageDto updated = pageService.updateFromMarkdownBySlug(slug, mdFile);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    // 删除（需要 ROOT）——按 slug 删除
    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/{slug}")
    public ResponseEntity<String> deletePage(@PathVariable String slug) {
        pageService.deleteBySlug(slug);
        return ResponseEntity.ok(slug);
    }
}
