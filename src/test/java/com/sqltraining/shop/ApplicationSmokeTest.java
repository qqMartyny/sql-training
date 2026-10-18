package com.sqltraining.shop;

import com.sqltraining.shop.support.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что все эндпоинты работают (пусть и неэффективно). Входит в обычный `mvn test`.
 * Если после рефакторинга этот тест упал — вы что-то сломали.
 */
class ApplicationSmokeTest extends IntegrationTest {

    private static final String YEAR_AGO = Instant.now().minus(365, ChronoUnit.DAYS).toString();
    private static final String NOW = Instant.now().toString();

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/orders/recent?limit=10",
            "/api/orders/1/items",
            "/api/orders/feed?page=3&size=20",
            "/api/customers/by-email?email=user1@mail.example",
            "/api/customers/1/stats",
            "/api/products/catalog?page=1&size=10",
            "/api/products/search?q=pro&minPrice=1000&inStock=true",
            "/api/products/1",
            "/api/categories/1/products-count",
            "/api/reports/top-customers?limit=5",
            "/api/reports/inactive-customers?days=90",
            "/api/reports/monthly-revenue?year=2025",
            "/api/reports/top-products?n=3",
    })
    void getEndpointsRespond(String uri) {
        String body = http().get().uri(uri).retrieve().body(String.class);
        assertThat(body).isNotBlank();
    }

    @org.junit.jupiter.api.Test
    void ordersByStatusAndPeriodRespond() {
        String body = http().get()
                .uri(b -> b.path("/api/orders").queryParam("status", "NEW")
                        .queryParam("from", YEAR_AGO).queryParam("to", NOW).build())
                .retrieve().body(String.class);
        assertThat(body).startsWith("[");
    }
}
