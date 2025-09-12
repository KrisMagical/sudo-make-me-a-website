package com.magiccode.backend.service;

import com.magiccode.backend.dto.CategoryDto;
import com.magiccode.backend.mapping.CategoryMapping;
import com.magiccode.backend.model.Category;
import com.magiccode.backend.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Data
@Transactional
public class CategoryService {
    private CategoryRepository categoryRepository;
    private CategoryMapping categoryMapping;

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapping::toCategoryDto)
                .toList();
    }

    public CategoryDto createCategory(CategoryDto categoryDto) {
        if (categoryRepository.existsByName(categoryDto.getName())) {
            throw new RuntimeException("Category name already exists!");
        }
        if (categoryRepository.existsBySlug(categoryDto.getSlug())) {
            throw new RuntimeException("Category slug already exists!");
        }
        Category category = categoryMapping.toCategoryEntity(categoryDto);
        Category category_save = categoryRepository.save(category);
        return categoryMapping.toCategoryDto(category_save);
    }

    public CategoryDto updateCategory(String name, CategoryDto categoryDto) {
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Category Not Found: " + name));
        category.setName(categoryDto.getName());
        category.setSlug(categoryDto.getSlug());
        Category category_update = categoryRepository.save(category);
        return categoryMapping.toCategoryDto(category_update);
    }

    public void deleteCategory(String name) {
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Category Not Found: " + name));
        categoryRepository.delete(category);
    }
}
