package com.sqltraining.shop.web;

import com.sqltraining.shop.domain.OrderStatus;
import com.sqltraining.shop.service.OrderService;
import com.sqltraining.shop.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/recent")
    public List<OrderSummaryDto> recent(@RequestParam(defaultValue = "50") int limit) {
        return orderService.findRecent(limit);
    }

    @GetMapping("/{id}/items")
    public List<OrderShortDto.Item> items(@PathVariable Long id) {
        return orderService.findItems(id);
    }

    @GetMapping
    public List<OrderShortDto> byStatusAndPeriod(@RequestParam OrderStatus status,
                                                 @RequestParam Instant from,
                                                 @RequestParam Instant to) {
        return orderService.findByStatusAndPeriod(status, from, to);
    }

    @GetMapping("/feed")
    public PageDto<OrderShortDto> feed(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return orderService.feed(page, size);
    }

    @GetMapping("/feed/keyset")
    public List<OrderShortDto> feedKeyset(@RequestParam(required = false) Instant afterCreatedAt,
                                          @RequestParam(required = false) Long afterId,
                                          @RequestParam(defaultValue = "20") int size) {
        return orderService.feedAfter(afterCreatedAt, afterId, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto place(@Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(request);
    }
}
