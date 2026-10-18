package com.sqltraining.shop.challenges;

import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.web.dto.ProductDetailsDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case14MultipleBagsTest extends IntegrationTest {

    private Long productWithMostReviewsAndTags() {
        return jdbc.queryForObject("""
                select r.product_id from reviews r
                where r.product_id in (select product_id from product_tags group by product_id having count(*) >= 2)
                group by r.product_id order by count(*) desc, r.product_id limit 1
                """, Long.class);
    }

    @Test
    void productDetailsAreLoadedWithFewQueries() {
        Long productId = productWithMostReviewsAndTags();

        resetStatistics();
        http().get().uri("/api/products/{id}", productId).retrieve().body(ProductDetailsDto.class);

        assertThat(sql.snapshot().roundTrips())
                .as("Товар с категорией и тегами, отзывы с авторами: %s", sql.snapshot())
                .isLessThanOrEqualTo(2);
    }

    @Test
    void productDetailsHaveNoDuplicates() {
        Long productId = productWithMostReviewsAndTags();

        ProductDetailsDto details = http().get().uri("/api/products/{id}", productId)
                .retrieve().body(ProductDetailsDto.class);

        assertThat(details.reviews()).hasSize(
                jdbc.queryForObject("select count(*) from reviews where product_id = ?", Integer.class, productId));
        assertThat(details.tags()).hasSize(
                jdbc.queryForObject("select count(*) from product_tags where product_id = ?", Integer.class, productId));
        assertThat(details.reviews()).extracting(ProductDetailsDto.ReviewDto::id).doesNotHaveDuplicates();
    }
}
