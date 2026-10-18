package com.sqltraining.shop.challenges;

import com.sqltraining.shop.service.CategoryService;
import com.sqltraining.shop.support.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case12RecursiveTreeTest extends IntegrationTest {

    @Autowired
    CategoryService categoryService;

    @Test
    void subtreeIsCountedInOneQuery() {
        Long rootId = jdbc.queryForObject("select min(id) from categories where parent_id is null", Long.class);

        resetStatistics();
        categoryService.countProductsInTree(rootId);

        assertThat(sql.snapshot().roundTrips()).isLessThanOrEqualTo(2);
    }

    @Test
    void subtreeCountIsCorrect() {
        Map<Long, List<Long>> children = new HashMap<>();
        jdbc.query("select id, parent_id from categories where parent_id is not null",
                rs -> {
                    children.computeIfAbsent(rs.getLong(2), k -> new ArrayList<>()).add(rs.getLong(1));
                });
        Map<Long, Long> productsByCategory = new HashMap<>();
        jdbc.query("select category_id, count(*) from products group by category_id",
                rs -> {
                    productsByCategory.put(rs.getLong(1), rs.getLong(2));
                });

        for (Long categoryId : jdbc.queryForList("select id from categories order by id limit 20", Long.class)) {
            long expected = 0;
            Deque<Long> stack = new ArrayDeque<>(List.of(categoryId));
            while (!stack.isEmpty()) {
                Long id = stack.pop();
                expected += productsByCategory.getOrDefault(id, 0L);
                stack.addAll(children.getOrDefault(id, List.of()));
            }
            assertThat(categoryService.countProductsInTree(categoryId).productsCount())
                    .as("category %d", categoryId).isEqualTo(expected);
        }
    }
}
