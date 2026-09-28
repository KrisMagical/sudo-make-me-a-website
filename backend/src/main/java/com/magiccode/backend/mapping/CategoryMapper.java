package com.magiccode.backend.mapping;

import com.magiccode.backend.dto.CategoryDto;
import com.magiccode.backend.model.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDto toCategoryDto(Category category);

    Category toCategoryEntity(CategoryDto dto);
}
