package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.BrowserIconDto;
import com.magiccode.backend.model.BrowserIcon;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrowserIconMapper {
    BrowserIconDto toBrowserIconDto(BrowserIcon browserIcon);
    BrowserIcon toBrowserIconEntity(BrowserIconDto dto);
}
