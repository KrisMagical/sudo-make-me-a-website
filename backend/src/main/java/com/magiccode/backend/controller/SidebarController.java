package com.magiccode.backend.controller;

import com.magiccode.backend.dto.BrowserIconDto;
import com.magiccode.backend.dto.SidebarDto;
import com.magiccode.backend.dto.SiteConfigDto;
import com.magiccode.backend.service.SidebarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sidebar")
public class SidebarController {
    private final SidebarService sidebarService;

    @GetMapping
    public ResponseEntity<SidebarDto> getSidebarData() {
        SidebarDto sidebarDto = sidebarService.getSidebarData();
        return ResponseEntity.ok(sidebarDto);
    }

    @GetMapping("/site-config")
    public ResponseEntity<SiteConfigDto> getSiteConfig() {
        SiteConfigDto siteConfigDto = sidebarService.getSiteConfig();
        return ResponseEntity.ok(siteConfigDto);
    }

    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/site-config")
    public ResponseEntity<SiteConfigDto> updateSiteConfig(@RequestBody SiteConfigDto dto) {
        SiteConfigDto updated = sidebarService.updateSiteConfig(dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/browser-icon")
    public ResponseEntity<BrowserIconDto> getBrowserIcon() {
        BrowserIconDto browserIconDto = sidebarService.getBrowserIcon();
        return ResponseEntity.ok(browserIconDto);
    }

    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/browser-icon")
    public ResponseEntity<BrowserIconDto> updateBrowserIcon(@RequestBody BrowserIconDto dto) {
        BrowserIconDto updated = sidebarService.updateBrowserIcon(dto);
        return ResponseEntity.ok(updated);
    }
}
