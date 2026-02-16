package com.magiccode.backend.controller;

import com.magiccode.backend.dto.CategoryDto;
import com.magiccode.backend.service.CategoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/categories")

public class CategoryController {
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        List<CategoryDto> categoryDto = categoryService.getAllCategories();
        if (categoryDto == null) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(categoryDto);
    }
    @PreAuthorize("hasRole('ROOT')")
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryDto categoryDto) {
        CategoryDto categoryDto_save = categoryService.createCategory(categoryDto);
        if (categoryDto_save == null) {
            throw new RuntimeException("Create Failed");
        }
        return new ResponseEntity<>(categoryDto_save, HttpStatus.OK);
    }
    @PreAuthorize("hasRole('ROOT')")
    @PutMapping("/{name}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable String name, @RequestBody CategoryDto categoryDto) {
        CategoryDto categoryDto_update = categoryService.updateCategory(name, categoryDto);
        if (categoryDto_update == null) {
            throw new RuntimeException("Update Failed");
        }
        return new ResponseEntity<>(categoryDto_update, HttpStatus.OK);
    }
    @PreAuthorize("hasRole('ROOT')")
    @DeleteMapping("/{name}")
    public ResponseEntity<String> deleteCategory(@PathVariable String name) {
        categoryService.deleteCategory(name);
        return ResponseEntity.ok(name);
    }
}
