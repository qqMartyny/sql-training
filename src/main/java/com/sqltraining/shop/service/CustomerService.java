package com.sqltraining.shop.service;

import com.sqltraining.shop.domain.Customer;
import com.sqltraining.shop.domain.Order;
import com.sqltraining.shop.domain.OrderStatus;
import com.sqltraining.shop.repository.CustomerRepository;
import com.sqltraining.shop.web.dto.CustomerDto;
import com.sqltraining.shop.web.dto.CustomerStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * CASE-06. Поиск клиента по email без учёта регистра (логин, поддержка).
     */
    @Transactional
    public CustomerDto findByEmail(String email) {
        Customer c = customerRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Customer", email));
        return new CustomerDto(c.getId(), c.getEmail(), c.getFirstName(), c.getLastName(), c.getCity());
    }

    /**
     * CASE-04. Статистика клиента для личного кабинета.
     */
    @Transactional
    public CustomerStatsDto getStats(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer", customerId));

        List<Order> orders = customer.getOrders();
        List<Order> paidOrders = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .toList();

        Map<OrderStatus, Long> byStatus = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        BigDecimal totalSpent = paidOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = paidOrders.isEmpty()
                ? BigDecimal.ZERO
                : totalSpent.divide(BigDecimal.valueOf(paidOrders.size()), 2, RoundingMode.HALF_UP);

        String favoriteCategory = paidOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(i -> i.getProduct().getCategory().getName(),
                        Collectors.summingInt(i -> i.getQuantity())))
                .entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey)
                .orElse(null);

        return new CustomerStatsDto(customerId, orders.size(), totalSpent, average, byStatus, favoriteCategory);
    }
}
