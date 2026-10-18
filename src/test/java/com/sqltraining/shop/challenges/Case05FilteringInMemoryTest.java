package com.sqltraining.shop.challenges;

import com.sqltraining.shop.domain.Product;
import com.sqltraining.shop.service.ProductService;
import com.sqltraining.shop.support.ExplainHelper;
import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.web.dto.ProductDto;
import com.sqltraining.shop.web.dto.ProductSearchFilter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case05FilteringInMemoryTest extends IntegrationTest {

    @Autowired
    ProductService productService;

    @Autowired
    TransactionTemplate tx;

    @Test
    void filteringIsDoneByDatabase() {
        var filter = new ProductSearchFilter("PRO", new BigDecimal("1000"), new BigDecimal("50000"), null, true);

        resetStatistics();
        List<ProductDto> found = productService.search(filter);

        assertThat(found).isNotEmpty();
        assertThat(loadedEntities(Product.class))
                .as("Загружено сущностей Product (найдено %d)", found.size())
                .isLessThanOrEqualTo(found.size());
        assertThat(sql.snapshot().roundTrips()).isEqualTo(1);
    }

    @Test
    void searchResultIsCorrect() {
        Long categoryId = jdbc.queryForObject(
                "select category_id from products group by category_id order by count(*) desc, category_id limit 1", Long.class);

        assertThat(productService.search(new ProductSearchFilter("pro", new BigDecimal("1000"), null, null, true)))
                .extracting(ProductDto::id)
                .containsExactlyInAnyOrderElementsOf(jdbc.queryForList(
                        "select id from products where name ilike '%pro%' and price >= 1000 and stock > 0", Long.class));

        assertThat(productService.search(new ProductSearchFilter(null, null, new BigDecimal("30000"), categoryId, null)))
                .extracting(ProductDto::id)
                .containsExactlyInAnyOrderElementsOf(jdbc.queryForList(
                        "select id from products where price <= 30000 and category_id = ?", Long.class, categoryId));

        assertThat(productService.search(new ProductSearchFilter(null, null, null, null, null)))
                .hasSize(jdbc.queryForObject("select count(*) from products", Integer.class));
    }

    @Test
    void substringSearchByNameCanUseIndex() {
        String plan = new ExplainHelper(jdbc, tx).explain(
                "select * from products where name ilike '%phone pro%'", Map.of(), true);

        assertThat(plan).as(plan).doesNotContain("Seq Scan on products");
    }
}
