package com.sqltraining.shop.web.dto;

import java.math.BigDecimal;

public record ProductDto(Long id, String sku, String name, BigDecimal price, int stock, Long categoryId) {
}
