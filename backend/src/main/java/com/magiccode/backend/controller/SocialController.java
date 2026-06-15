package com.magiccode.backend.controller;

import com.magiccode.backend.config.OpenApiConfig;
import com.magiccode.backend.dto.SocialDto;
import com.magiccode.backend.service.SocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Public Social")
public class SocialController {
    private final SocialService socialService;

    @Operation(summary = "Create social link", description = "Creates a social link as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping(value = "/create",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SocialDto> create(@RequestPart("data") SocialDto dto,
                                            @RequestPart(value = "iconFile", required = false) MultipartFile iconFile,
                                            @RequestParam(value = "externalIconUrl", required = false) String externalIconUrl) {
        SocialDto created = socialService.create(dto, iconFile, externalIconUrl);
        return ResponseEntity.created(URI.create("/api/socials/" + created.getId()))
                .body(created);
    }

    @Operation(summary = "Update social link", description = "Updates a social link as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PutMapping(value = "/update/{id}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SocialDto> update(@PathVariable Long id,
                                            @RequestPart("data") SocialDto dto,
                                            @RequestPart(value = "iconFile", required = false) MultipartFile iconFile,
                                            @RequestParam(value = "externalIconUrl", required = false) String externalIconUrl) {
        SocialDto updated = socialService.update(id, dto, iconFile, externalIconUrl);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete social link", description = "Deletes a social link as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        socialService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List social links", description = "Returns public social links.")
    @GetMapping
    public ResponseEntity<List<SocialDto>> listAll() {
        return ResponseEntity.ok(socialService.listAll());
    }
}
