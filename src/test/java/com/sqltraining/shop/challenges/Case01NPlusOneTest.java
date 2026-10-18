package com.sqltraining.shop.challenges;

import com.sqltraining.shop.service.OrderService;
import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.web.dto.OrderSummaryDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case01NPlusOneTest extends IntegrationTest {

    @Autowired
    OrderService orderService;

    @Test
    void recentOrdersAreLoadedWithConstantNumberOfQueries() {
        resetStatistics();
        List<OrderSummaryDto> recent = orderService.findRecent(50);

        assertThat(sql.snapshot().roundTrips())
                .as("Количество обращений к БД не должно зависеть от количества заказов")
                .isLessThanOrEqualTo(3);
        assertThat(recent).hasSize(50);
    }

    @Test
    void resultIsStillCorrect() {
        List<OrderSummaryDto> recent = orderService.findRecent(20);

        List<Map<String, Object>> expected = jdbc.queryForList("""
                select o.id, count(*) as items, sum(oi.price * oi.quantity) as total
                from (select id, created_at from orders order by created_at desc limit 20) o
                join order_items oi on oi.order_id = o.id
                group by o.id, o.created_at
                order by o.created_at desc
                """);
        assertThat(recent).extracting(OrderSummaryDto::id)
                .containsExactlyElementsOf(expected.stream().map(r -> ((Number) r.get("id")).longValue()).toList());
        for (int i = 0; i < recent.size(); i++) {
            assertThat(recent.get(i).itemsCount()).isEqualTo(((Number) expected.get(i).get("items")).intValue());
            assertThat(recent.get(i).total()).isEqualByComparingTo((BigDecimal) expected.get(i).get("total"));
            assertThat(recent.get(i).productNames()).hasSize(recent.get(i).itemsCount());
        }
    }
}
