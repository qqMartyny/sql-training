package com.sqltraining.shop.challenges;

import com.sqltraining.shop.domain.Order;
import com.sqltraining.shop.domain.OrderItem;
import com.sqltraining.shop.domain.OrderStatus;
import com.sqltraining.shop.service.CustomerService;
import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.web.dto.CustomerStatsDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case04AggregationInJavaTest extends IntegrationTest {

    @Autowired
    CustomerService customerService;

    private Long customerWithMostOrders() {
        return jdbc.queryForObject(
                "select customer_id from orders group by customer_id order by count(*) desc, customer_id limit 1", Long.class);
    }

    @Test
    void statsAreCalculatedByDatabase() {
        Long customerId = customerWithMostOrders();

        resetStatistics();
        customerService.getStats(customerId);

        assertThat(loadedEntities(Order.class)).as("Загружено сущностей Order").isZero();
        assertThat(loadedEntities(OrderItem.class)).as("Загружено сущностей OrderItem").isZero();
        assertThat(sql.snapshot().roundTrips()).isLessThanOrEqualTo(3);
    }

    @Test
    void statsAreCorrect() {
        Long customerId = customerWithMostOrders();
        CustomerStatsDto stats = customerService.getStats(customerId);

        long orders = jdbc.queryForObject("select count(*) from orders where customer_id = ?", Long.class, customerId);
        long cancelled = jdbc.queryForObject(
                "select count(*) from orders where customer_id = ? and status = 'CANCELLED'", Long.class, customerId);
        BigDecimal spent = jdbc.queryForObject("""
                select coalesce(sum(oi.price * oi.quantity), 0) from orders o join order_items oi on oi.order_id = o.id
                where o.customer_id = ? and o.status <> 'CANCELLED'
                """, BigDecimal.class, customerId);
        String favorite = jdbc.queryForObject("""
                select c.name from orders o
                join order_items oi on oi.order_id = o.id
                join products p on p.id = oi.product_id
                join categories c on c.id = p.category_id
                where o.customer_id = ? and o.status <> 'CANCELLED'
                group by c.name order by sum(oi.quantity) desc, c.name limit 1
                """, String.class, customerId);

        assertThat(stats.ordersCount()).isEqualTo(orders);
        assertThat(stats.ordersByStatus().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(orders);
        assertThat(stats.ordersByStatus().getOrDefault(OrderStatus.CANCELLED, 0L)).isEqualTo(cancelled);
        assertThat(stats.totalSpent()).isEqualByComparingTo(spent);
        assertThat(stats.favoriteCategory()).isEqualTo(favorite);
    }
}
