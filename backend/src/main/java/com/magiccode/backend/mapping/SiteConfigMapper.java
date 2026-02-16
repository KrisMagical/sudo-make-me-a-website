package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.SiteConfigDto;
import com.magiccode.backend.model.SiteConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SiteConfigMapper {
    SiteConfigDto toSiteConfigDto(SiteConfig siteConfig);
    SiteConfig toSiteConfigEntity(SiteConfigDto dto);
}
