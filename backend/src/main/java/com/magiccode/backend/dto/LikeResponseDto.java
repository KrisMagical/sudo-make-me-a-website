package com.magiccode.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Reaction count response.")
public class LikeResponseDto {
    @Schema(description = "Current like count.", example = "5")
    private int likes;
    @Schema(description = "Current dislike count.", example = "1")
    private int dislikes;
    @Schema(description = "Operation result message.", example = "Success")
    private String message;

    public LikeResponseDto(int likes, int dislikes) {
        this.likes = likes;
        this.dislikes = dislikes;
        this.message = "Success";
    }
}
