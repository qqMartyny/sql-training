package com.sqltraining.shop.service;

import com.sqltraining.shop.domain.*;
import com.sqltraining.shop.integration.PaymentGateway;
import com.sqltraining.shop.repository.CustomerRepository;
import com.sqltraining.shop.repository.OrderItemRepository;
import com.sqltraining.shop.repository.OrderRepository;
import com.sqltraining.shop.repository.ProductRepository;
import com.sqltraining.shop.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentGateway paymentGateway;

    /**
     * CASE-01. Последние заказы для дашборда менеджера.
     */
    @Transactional
    public List<OrderSummaryDto> findRecent(int limit) {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc(Limit.of(limit));
        return orders.stream()
                .map(order -> new OrderSummaryDto(
                        order.getId(),
                        order.getCreatedAt(),
                        order.getStatus(),
                        order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName(),
                        order.getCustomer().getEmail(),
                        order.getItems().size(),
                        total(order),
                        order.getItems().stream().map(i -> i.getProduct().getName()).toList()))
                .toList();
    }

    /**
     * CASE-02. Позиции одного заказа.
     */
    @Transactional
    public List<OrderShortDto.Item> findItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(i -> new OrderShortDto.Item(i.getId(), i.getProduct().getId(), i.getQuantity(), i.getPrice()))
                .toList();
    }

    /**
     * CASE-06. Поиск заказов по статусу за период (например, «все NEW за последние сутки»).
     */
    @Transactional
    public List<OrderShortDto> findByStatusAndPeriod(OrderStatus status, Instant from, Instant to) {
        return orderRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(status, from, to).stream()
                .map(this::toShort)
                .toList();
    }

    /**
     * CASE-07. Лента заказов с классической offset-пагинацией.
     */
    @Transactional
    public PageDto<OrderShortDto> feed(int page, int size) {
        Page<Order> orders = orderRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return PageDto.of(orders.map(this::toShort));
    }

    /**
     * CASE-07. Лента заказов с keyset-пагинацией (seek method): следующая страница
     * начинается сразу после заказа (afterCreatedAt, afterId). Если курсор null — первая страница.
     * Порядок сортировки тот же, что и в {@link #feed(int, int)}.
     */
    public List<OrderShortDto> feedAfter(Instant afterCreatedAt, Long afterId, int size) {
        throw new UnsupportedOperationException("CASE-07: реализуйте keyset-пагинацию");
    }

    /**
     * CASE-10. Оформление заказа: проверяем остатки, списываем их, авторизуем платёж.
     */
    @Transactional
    public OrderDto placeOrder(PlaceOrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer", request.customerId()));

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.NEW);
        order.setCreatedAt(Instant.now());

        for (PlaceOrderRequest.Line line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Product", line.productId()));
            if (product.getStock() < line.quantity()) {
                throw new OutOfStockException(product.getId());
            }
            product.setStock(product.getStock() - line.quantity());

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(line.quantity());
            item.setPrice(product.getPrice());
            order.addItem(item);
        }

        BigDecimal total = total(order);
        paymentGateway.authorize(customer.getId(), total);
        order.setStatus(OrderStatus.PAID);

        orderRepository.save(order);
        return new OrderDto(order.getId(), order.getStatus(), total);
    }

    private OrderShortDto toShort(Order order) {
        return new OrderShortDto(order.getId(), order.getCreatedAt(), order.getStatus(), order.getCustomer().getId());
    }

    private static BigDecimal total(Order order) {
        return order.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
