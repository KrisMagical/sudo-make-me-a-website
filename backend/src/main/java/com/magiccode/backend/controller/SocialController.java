package com.magiccode.backend.controller;

import com.magiccode.backend.dto.SocialDto;
import com.magiccode.backend.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/socials")
public class SocialController {
    private final SocialService socialService;

    @PostMapping(value = "/create",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SocialDto> create(@RequestPart("data") SocialDto dto,
                                            @RequestPart(value = "iconFile", required = false) MultipartFile iconFile,
                                            @RequestParam(value = "externalIconUrl", required = false) String externalIconUrl) {
        SocialDto created = socialService.create(dto, iconFile, externalIconUrl);
        return ResponseEntity.created(URI.create("/api/socials/" + created.getId()))
                .body(created);
    }

    @PutMapping(value = "/update/{id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SocialDto> update(@PathVariable Long id,
                                            @RequestPart("data") SocialDto dto,
                                            @RequestPart(value = "iconFile", required = false) MultipartFile iconFile,
                                            @RequestParam(value = "externalIconUrl", required = false) String externalIconUrl) {
        SocialDto updated = socialService.update(id, dto, iconFile, externalIconUrl);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        socialService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SocialDto>> listAll() {
        return ResponseEntity.ok(socialService.listAll());
    }
}
