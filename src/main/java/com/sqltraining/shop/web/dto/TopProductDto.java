package com.sqltraining.shop.web.dto;

public record TopProductDto(Long categoryId, String categoryName, Long productId, String productName, long soldQuantity) {
}
