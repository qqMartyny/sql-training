package com.sqltraining.shop.challenges;

import com.sqltraining.shop.service.OrderService;
import com.sqltraining.shop.support.ExplainHelper;
import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.web.dto.OrderShortDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case07KeysetPaginationTest extends IntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    TransactionTemplate tx;

    @Test
    void keysetPagesMatchOffsetPages() {
        List<OrderShortDto> first = orderService.feedAfter(null, null, 20);
        assertThat(first).containsExactlyElementsOf(orderService.feed(0, 20).content());

        OrderShortDto last = first.getLast();
        assertThat(orderService.feedAfter(last.createdAt(), last.id(), 20))
                .containsExactlyElementsOf(orderService.feed(1, 20).content());

        OrderShortDto deep = orderService.feed(100, 20).content().getLast();
        assertThat(orderService.feedAfter(deep.createdAt(), deep.id(), 20))
                .containsExactlyElementsOf(orderService.feed(101, 20).content());
    }

    @Test
    void keysetPageIsOneQuery() {
        OrderShortDto cursor = orderService.feed(100, 20).content().getLast();

        resetStatistics();
        orderService.feedAfter(cursor.createdAt(), cursor.id(), 20);

        assertThat(sql.snapshot().roundTrips()).as("Ни count(*), ни догрузки связей").isEqualTo(1);
    }

    @Test
    void feedOrderIsSupportedByIndex() {
        String plan = new ExplainHelper(jdbc, tx).explain(
                "select * from orders order by created_at desc, id desc limit 20", Map.of(), true);

        assertThat(plan).as(plan).doesNotContain("Sort").doesNotContain("Seq Scan on orders");
    }
}
