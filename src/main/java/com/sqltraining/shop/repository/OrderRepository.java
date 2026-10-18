package com.sqltraining.shop.repository;

import com.sqltraining.shop.domain.Order;
import com.sqltraining.shop.domain.OrderStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderByCreatedAtDesc(Limit limit);

    List<Order> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(OrderStatus status, Instant from, Instant to);

    @Query("""
            select coalesce(sum(i.price * i.quantity), 0)
            from Order o join o.items i
            where o.status <> com.sqltraining.shop.domain.OrderStatus.CANCELLED
              and o.createdAt >= :from and o.createdAt < :to
            """)
    BigDecimal sumRevenue(@Param("from") Instant from, @Param("to") Instant to);
}
