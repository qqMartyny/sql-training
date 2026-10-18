package com.sqltraining.shop.integration;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Заглушка внешнего платёжного сервиса. Сетевой вызов занимает ~100 мс.
 */
@Component
public class PaymentGateway {

    public void authorize(Long customerId, BigDecimal amount) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Payment authorization interrupted", e);
        }
    }
}
