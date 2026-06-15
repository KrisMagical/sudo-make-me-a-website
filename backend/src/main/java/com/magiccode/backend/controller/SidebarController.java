package com.magiccode.backend.controller;

import com.magiccode.backend.config.OpenApiConfig;
import com.magiccode.backend.dto.BrowserIconDto;
import com.magiccode.backend.dto.SidebarDto;
import com.magiccode.backend.dto.SiteConfigDto;
import com.magiccode.backend.service.SidebarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sidebar")
@Tag(name = "Admin Config")
public class SidebarController {
    private final SidebarService sidebarService;

    @Operation(summary = "Get sidebar data", description = "Returns public sidebar configuration, categories, and browser icon data.")
    @GetMapping
    public ResponseEntity<SidebarDto> getSidebarData() {
        SidebarDto sidebarDto = sidebarService.getSidebarData();
        return ResponseEntity.ok(sidebarDto);
    }

    @Operation(summary = "Get site config", description = "Returns public site configuration.")
    @GetMapping("/site-config")
    public ResponseEntity<SiteConfigDto> getSiteConfig() {
        SiteConfigDto siteConfigDto = sidebarService.getSiteConfig();
        return ResponseEntity.ok(siteConfigDto);
    }

    @Operation(summary = "Update site config", description = "Updates site configuration as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/site-config")
    public ResponseEntity<SiteConfigDto> updateSiteConfig(@RequestBody SiteConfigDto dto) {
        SiteConfigDto updated = sidebarService.updateSiteConfig(dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Get browser icon config", description = "Returns public favicon and Apple touch icon configuration.")
    @GetMapping("/browser-icon")
    public ResponseEntity<BrowserIconDto> getBrowserIcon() {
        BrowserIconDto browserIconDto = sidebarService.getBrowserIcon();
        return ResponseEntity.ok(browserIconDto);
    }

    @Operation(summary = "Update browser icon config", description = "Updates browser icon configuration as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/browser-icon")
    public ResponseEntity<BrowserIconDto> updateBrowserIcon(@RequestBody BrowserIconDto dto) {
        BrowserIconDto updated = sidebarService.updateBrowserIcon(dto);
        return ResponseEntity.ok(updated);
    }
}
