package com.magiccode.backend.controller;

import com.magiccode.backend.config.OpenApiConfig;
import com.magiccode.backend.dto.HomeProfileDto;
import com.magiccode.backend.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
@Tag(name = "Public Social")
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "Get home profile", description = "Returns public home page profile content.")
    @GetMapping
    public ResponseEntity<HomeProfileDto> getHome() {
        return ResponseEntity.ok(homeService.getHome());
    }

    @Operation(summary = "Update home profile", description = "Updates home page profile content as an authenticated admin.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/update")
    public ResponseEntity<HomeProfileDto> updateHome(@RequestBody HomeProfileDto dto) {
        return ResponseEntity.ok(homeService.updateHome(dto));
    }
}
