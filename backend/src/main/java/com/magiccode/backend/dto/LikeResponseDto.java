package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeResponseDto {
    private int likes;
    private int dislikes;
    private String message;

    public LikeResponseDto(int likes, int dislikes) {
        this.likes = likes;
        this.dislikes = dislikes;
        this.message = "Success";
    }
}
