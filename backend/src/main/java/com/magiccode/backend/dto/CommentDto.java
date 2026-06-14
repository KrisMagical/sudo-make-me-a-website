package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.magiccode.backend.model.CommentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private String name;
    private String content;
    private LocalDateTime createdAt;
    private Long parentId;
    private boolean author;
    private CommentStatus status;
}
