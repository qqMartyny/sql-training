package com.sqltraining.shop.repository;

import com.sqltraining.shop.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("select coalesce(sum(i.quantity), 0) from OrderItem i where i.product.id = :productId")
    long sumSoldQuantity(@Param("productId") Long productId);
}
