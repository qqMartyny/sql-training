package com.sqltraining.shop.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductDetailsDto(Long id,
                                String sku,
                                String name,
                                String description,
                                BigDecimal price,
                                int stock,
                                String categoryName,
                                List<String> tags,
                                List<ReviewDto> reviews) {

    public record ReviewDto(Long id, short rating, String comment, String authorName, Instant createdAt) {
    }
}
