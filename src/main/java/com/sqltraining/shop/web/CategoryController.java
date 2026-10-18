package com.sqltraining.shop.web;

import com.sqltraining.shop.service.CategoryService;
import com.sqltraining.shop.web.dto.CategoryTreeStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/{id}/products-count")
    public CategoryTreeStatsDto productsCount(@PathVariable Long id) {
        return categoryService.countProductsInTree(id);
    }
}
