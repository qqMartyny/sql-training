package com.sqltraining.shop.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PlaceOrderRequest(@NotNull Long customerId, @NotEmpty List<@Valid Line> items) {

    public record Line(@NotNull Long productId, @Positive int quantity) {
    }
}
