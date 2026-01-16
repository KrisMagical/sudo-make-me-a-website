package com.magiccode.backend.dto;

import com.magiccode.backend.model.HomeMedia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeMediaDto {
    private Long id;
    private HomeMedia.MediaType type;
    private String url;
    private String caption;
    private Integer orderIndex;
}
