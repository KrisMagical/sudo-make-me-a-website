package com.magiccode.backend.controller;

import com.magiccode.backend.dto.MovePageRequest;
import com.magiccode.backend.dto.PageDto;
import com.magiccode.backend.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pages")
public class PageController {
    private final PageService pageService;

    @GetMapping("/{slug}")
    public ResponseEntity<PageDto> getPageBySlug(@PathVariable String slug) {
        PageDto pageDto = pageService.getPageBySlug(slug);
        return new ResponseEntity<>(pageDto, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<PageDto>> listPages() {
        return ResponseEntity.ok(pageService.listAll());
    }

    @PreAuthorize("hasRole('ROOT')")
    @PostMapping("/create")
    public ResponseEntity<PageDto> createPage(@RequestBody PageDto dto) {
        PageDto created = pageService.createPage(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }


    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/update/{slug}")
    public ResponseEntity<PageDto> updatePageBySlug(@PathVariable String slug, @RequestBody PageDto dto) {
        PageDto updated = pageService.updateBySlug(slug, dto);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }


    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/{slug}")
    public ResponseEntity<String> deletePage(@PathVariable String slug) {
        pageService.deleteBySlug(slug);
        return ResponseEntity.ok(slug);
    }

    @GetMapping("/{slug}/backlinks")
    public ResponseEntity<List<PageDto>> backlinks(@PathVariable String slug) {
        return ResponseEntity.ok(pageService.listBackLinks(slug));
    }

    @GetMapping("/{slug}/outlinks")
    public ResponseEntity<List<PageDto>> outlinks(@PathVariable String slug) {
        return ResponseEntity.ok(pageService.listOutlinks(slug));
    }

    @PreAuthorize("hasRole('ROOT')")
    @PatchMapping("/{slug}/move")
    public ResponseEntity<PageDto> move(@PathVariable String slug, @RequestBody MovePageRequest request) {
        return ResponseEntity.ok(pageService.moveBySlug(slug, request));
    }
}
