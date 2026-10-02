package com.supermarket.web;

import com.supermarket.dto.CategoryDtos.CategoryDto;
import com.supermarket.dto.CategoryDtos.CategoryRequest;
import com.supermarket.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Product categories. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<CategoryDto> list() {
        return catalogService.listCategories();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public CategoryDto create(@Valid @RequestBody CategoryRequest request) {
        return catalogService.createCategory(request);
    }
}
