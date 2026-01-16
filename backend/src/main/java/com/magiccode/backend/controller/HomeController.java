package com.magiccode.backend.controller;

import com.magiccode.backend.dto.HomeMediaDto;
import com.magiccode.backend.dto.HomeProfileDto;
import com.magiccode.backend.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PutMapping
    public ResponseEntity<HomeProfileDto> updateHome(@RequestBody HomeProfileDto dto) {
        return ResponseEntity.ok(homeService.updateHome(dto));
    }

    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/media/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomeMediaDto> uploadImage(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(required = false) String caption,
                                                    @RequestParam(required = false) Integer orderIndex) {
        return ResponseEntity.ok(homeService.uploadHomeImage(file, caption, orderIndex));
    }

    @PreAuthorize("hasRole('ROOT')")
    @PostMapping(value = "/media/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomeMediaDto> uploadVideo(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(required = false) String caption,
                                                    @RequestParam(required = false) Integer orderIndex) {
        return ResponseEntity.ok(homeService.uploadHomeVideo(file, caption, orderIndex));
    }

    @PreAuthorize("hasRole('ROOT')")
    @PatchMapping("/media/{mediaId}")
    public ResponseEntity<HomeMediaDto> updateMedia(@PathVariable("mediaId") Long mediaId,
                                                    @RequestBody HomeMediaDto dto) {
        return ResponseEntity.ok(homeService.updateMedia(mediaId, dto));
    }

    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<String> deleteMedia(@PathVariable("mediaId") Long mediaId) {
        homeService.deleteMedia(mediaId);
        return ResponseEntity.ok("deleted: " + mediaId);
    }
}