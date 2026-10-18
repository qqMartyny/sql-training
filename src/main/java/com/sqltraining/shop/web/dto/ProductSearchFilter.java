package com.sqltraining.shop.web.dto;

import java.math.BigDecimal;

/**
 * Все поля необязательные: null означает «не фильтровать».
 */
public record ProductSearchFilter(String q,
                                  BigDecimal minPrice,
                                  BigDecimal maxPrice,
                                  Long categoryId,
                                  Boolean inStock) {
}
