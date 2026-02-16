package com.magiccode.backend.controller;

import com.magiccode.backend.dto.HomeProfileDto;
import com.magiccode.backend.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ResponseEntity<HomeProfileDto> getHome() {
        return ResponseEntity.ok(homeService.getHome());
    }

    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/update")
    public ResponseEntity<HomeProfileDto> updateHome(@RequestBody HomeProfileDto dto) {
        return ResponseEntity.ok(homeService.updateHome(dto));
    }
}