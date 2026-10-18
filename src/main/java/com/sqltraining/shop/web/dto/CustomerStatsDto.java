package com.sqltraining.shop.web.dto;

import com.sqltraining.shop.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;

public record CustomerStatsDto(Long customerId,
                               long ordersCount,
                               BigDecimal totalSpent,
                               BigDecimal averageOrderValue,
                               Map<OrderStatus, Long> ordersByStatus,
                               String favoriteCategory) {
}
