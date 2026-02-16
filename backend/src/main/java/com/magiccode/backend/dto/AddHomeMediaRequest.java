package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddHomeMediaRequest {
    private String type;
    private String url;
    private String caption;
    private Integer orderIndex;
}
