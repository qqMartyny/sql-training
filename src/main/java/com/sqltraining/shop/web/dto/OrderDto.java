package com.sqltraining.shop.web.dto;

import com.sqltraining.shop.domain.OrderStatus;

import java.math.BigDecimal;

public record OrderDto(Long id, OrderStatus status, BigDecimal total) {
}
