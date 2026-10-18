package com.sqltraining.shop.challenges;

import com.sqltraining.shop.service.ProductService;
import com.sqltraining.shop.support.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case09BulkUpdateTest extends IntegrationTest {

    @Autowired
    ProductService productService;

    /** Отдельная категория со 100 товарами, чтобы не трогать данные других тестов. */
    private Long createCategoryWithProducts() {
        Long categoryId = jdbc.queryForObject(
                "insert into categories (name) values ('Reprice ' || gen_random_uuid()) returning id", Long.class);
        jdbc.update("""
                insert into products (sku, name, price, stock, category_id, created_at)
                select 'RP-' || substr(gen_random_uuid()::text, 1, 20), 'Reprice item ' || g, 100 + g * 1.37, 10, ?, now()
                from generate_series(1, 100) g
                """, categoryId);
        return categoryId;
    }

    @Test
    void repriceIsSingleUpdateStatement() {
        Long categoryId = createCategoryWithProducts();

        resetStatistics();
        var result = productService.reprice(categoryId, new BigDecimal("10"));

        assertThat(result.updated()).isEqualTo(100);
        assertThat(sql.snapshot().roundTrips())
                .as("Массовое изменение цен: %s", sql.snapshot())
                .isLessThanOrEqualTo(2);
    }

    @Test
    void repriceIsCorrect() {
        Long categoryId = createCategoryWithProducts();
        List<BigDecimal> expected = jdbc.queryForList(
                "select round(price * 0.85, 2) from products where category_id = ? order by id", BigDecimal.class, categoryId);

        productService.reprice(categoryId, new BigDecimal("-15"));

        List<BigDecimal> actual = jdbc.queryForList(
                "select price from products where category_id = ? order by id", BigDecimal.class, categoryId);
        assertThat(actual).usingElementComparator(BigDecimal::compareTo).containsExactlyElementsOf(expected);
    }
}
