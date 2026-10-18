package com.sqltraining.shop.web.dto;

import com.sqltraining.shop.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderShortDto(Long id, Instant createdAt, OrderStatus status, Long customerId) {

    public record Item(Long id, Long productId, int quantity, BigDecimal price) {
    }
}
