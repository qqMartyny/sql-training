package com.sqltraining.shop.web.dto;

import java.math.BigDecimal;

public record CatalogItemDto(Long id,
                             String name,
                             BigDecimal price,
                             String categoryName,
                             long reviewsCount,
                             Double averageRating) {
}
