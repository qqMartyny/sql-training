package com.sqltraining.shop.web.dto;

import com.sqltraining.shop.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderSummaryDto(Long id,
                              Instant createdAt,
                              OrderStatus status,
                              String customerName,
                              String customerEmail,
                              int itemsCount,
                              BigDecimal total,
                              List<String> productNames) {
}
