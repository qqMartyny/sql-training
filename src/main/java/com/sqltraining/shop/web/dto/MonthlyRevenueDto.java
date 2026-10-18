package com.sqltraining.shop.web.dto;

import java.math.BigDecimal;

public record MonthlyRevenueDto(int month, BigDecimal revenue) {
}
