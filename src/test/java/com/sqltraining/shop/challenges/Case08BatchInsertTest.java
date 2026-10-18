package com.sqltraining.shop.challenges;

import com.sqltraining.shop.service.ImportService;
import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.support.SqlStatementCounter.SqlStats;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case08BatchInsertTest extends IntegrationTest {

    @Autowired
    ImportService importService;

    @Test
    void importUsesJdbcBatching() {
        Long categoryId = jdbc.queryForObject("select min(category_id) from products", Long.class);
        long before = jdbc.queryForObject("select count(*) from products", Long.class);

        resetStatistics();
        importService.importProducts(categoryId, 500);
        SqlStats stats = sql.snapshot();

        assertThat(jdbc.queryForObject("select count(*) from products", Long.class)).isEqualTo(before + 500);
        assertThat(stats.inserts()).isEqualTo(500);
        assertThat(stats.roundTrips())
                .as("500 вставок должны уйти в БД пачками, а не по одной: %s", stats)
                .isLessThanOrEqualTo(30);
    }
}
