package com.sqltraining.shop.service;

import com.sqltraining.shop.domain.Category;
import com.sqltraining.shop.domain.Product;
import com.sqltraining.shop.repository.CategoryRepository;
import com.sqltraining.shop.repository.ProductRepository;
import com.sqltraining.shop.web.dto.ImportResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * CASE-08. Импорт товаров от поставщика (здесь — сгенерированных) в указанную категорию.
     */
    public ImportResultDto importProducts(Long categoryId, int count) {
        long start = System.nanoTime();
        for (int i = 0; i < count; i++) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category", categoryId));

            Product product = new Product();
            product.setSku("IMP-" + UUID.randomUUID().toString().substring(0, 18));
            product.setName("Imported product " + i);
            product.setDescription("Imported from supplier feed");
            product.setPrice(BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(100_00, 10_000_00), 2));
            product.setStock(ThreadLocalRandom.current().nextInt(0, 100));
            product.setCategory(category);
            product.setCreatedAt(Instant.now());
            productRepository.save(product);
        }
        return new ImportResultDto(count, (System.nanoTime() - start) / 1_000_000);
    }
}
