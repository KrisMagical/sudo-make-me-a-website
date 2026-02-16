package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialDto {
    private Long id;
    private String name;
    private String url;
    private String description;
    private String iconUrl;
    private Long iconImageId;
    private String externalIconUrl;
}
