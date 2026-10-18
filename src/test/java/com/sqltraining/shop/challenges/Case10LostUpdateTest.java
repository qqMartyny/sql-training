package com.sqltraining.shop.challenges;

import com.sqltraining.shop.service.OrderService;
import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.web.dto.PlaceOrderRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case10LostUpdateTest extends IntegrationTest {

    private static final int STOCK = 5;
    private static final int BUYERS = 20;

    @Autowired
    OrderService orderService;

    @Test
    void concurrentOrdersNeverOversell() throws Exception {
        Long productId = jdbc.queryForObject("""
                insert into products (sku, name, price, stock, category_id, created_at)
                values ('HOT-' || substr(gen_random_uuid()::text, 1, 20), 'Limited edition', 999.00, ?,
                        (select min(id) from categories), now())
                returning id
                """, Long.class, STOCK);
        List<Long> customers = jdbc.queryForList("select id from customers order by id limit ?", Long.class, BUYERS);

        AtomicInteger succeeded = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(BUYERS)) {
            for (Long customerId : customers) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        orderService.placeOrder(new PlaceOrderRequest(customerId,
                                List.of(new PlaceOrderRequest.Line(productId, 1))));
                        succeeded.incrementAndGet();
                    } catch (RuntimeException e) {
                        // отказ покупателю — допустимый исход, если товар закончился
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        }

        int stockLeft = jdbc.queryForObject("select stock from products where id = ?", Integer.class, productId);
        int sold = jdbc.queryForObject(
                "select coalesce(sum(quantity), 0) from order_items where product_id = ?", Integer.class, productId);

        assertThat(sold).as("Продано единиц (на складе было %d)", STOCK).isLessThanOrEqualTo(STOCK);
        assertThat(sold).as("Успешных заказов").isEqualTo(succeeded.get());
        assertThat(stockLeft).as("Остаток должен сходиться с продажами").isEqualTo(STOCK - sold);
        assertThat(sold).as("Пока товар есть, покупателям не должны отказывать").isEqualTo(STOCK);
    }
}
