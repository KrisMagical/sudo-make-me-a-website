package com.magiccode.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrowserIconDto {
    private Long id;
    private Long faviconImageId;
    private String faviconUrl;
    private Long appleTouchIconImageId;
    private String appleTouchIconUrl;
    private Boolean isActive;
}
