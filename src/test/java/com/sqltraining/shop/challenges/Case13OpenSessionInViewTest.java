package com.sqltraining.shop.challenges;

import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.web.dto.ProductDetailsDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тот же эндпоинт, но с выключенным Open Session In View.
 */
@Tag("challenge")
@TestPropertySource(properties = "spring.jpa.open-in-view=false")
class Case13OpenSessionInViewTest extends IntegrationTest {

    @Test
    void productDetailsWorkWithoutOpenSessionInView() {
        Long productId = jdbc.queryForObject("select min(product_id) from reviews", Long.class);

        ProductDetailsDto details = http().get().uri("/api/products/{id}", productId)
                .retrieve().body(ProductDetailsDto.class);

        assertThat(details).isNotNull();
        assertThat(details.reviews()).isNotEmpty();
        assertThat(details.categoryName()).isNotBlank();
    }
}
