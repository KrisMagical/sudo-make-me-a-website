package com.magiccode.backend.controller;

import com.magiccode.backend.dto.VideoDto;
import com.magiccode.backend.mapping.VideoMapper;
import com.magiccode.backend.model.EmbeddedVideo;
import com.magiccode.backend.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos")
public class VideoController {
    private final VideoService videoService;
    private final VideoMapper videoMapper;

    @GetMapping("/{ownerType}/{ownerId}")
    public ResponseEntity<List<VideoDto>> list(@PathVariable EmbeddedVideo.OwnerType ownerType,
                                               @PathVariable Long ownerId) {
        return ResponseEntity.ok(videoMapper.toDtoList(videoService.list(ownerType, ownerId)));
    }
}
