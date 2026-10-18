package com.sqltraining.shop.service;

import com.sqltraining.shop.domain.Category;
import com.sqltraining.shop.repository.CategoryRepository;
import com.sqltraining.shop.repository.ProductRepository;
import com.sqltraining.shop.web.dto.CategoryTreeStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * CASE-12. Сколько товаров в категории вместе со всеми её подкатегориями (на любую глубину).
     */
    @Transactional
    public CategoryTreeStatsDto countProductsInTree(Long categoryId) {
        Category root = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category", categoryId));
        return new CategoryTreeStatsDto(categoryId, countRecursively(root));
    }

    private long countRecursively(Category category) {
        long count = productRepository.countByCategoryId(category.getId());
        for (Category child : category.getChildren()) {
            count += countRecursively(child);
        }
        return count;
    }
}
