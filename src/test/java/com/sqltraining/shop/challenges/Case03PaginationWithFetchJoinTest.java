package com.sqltraining.shop.challenges;

import com.sqltraining.shop.domain.Product;
import com.sqltraining.shop.domain.Review;
import com.sqltraining.shop.service.ProductService;
import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.web.dto.CatalogItemDto;
import com.sqltraining.shop.web.dto.PageDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case03PaginationWithFetchJoinTest extends IntegrationTest {

    @Autowired
    ProductService productService;

    @Test
    void catalogPageDoesNotLoadReviews() {
        resetStatistics();
        PageDto<CatalogItemDto> page = productService.catalog(1, 10);

        assertThat(page.content()).hasSize(10);
        assertThat(loadedEntities(Product.class))
                .as("Сколько сущностей Product подняли в память ради страницы из 10 товаров")
                .isLessThanOrEqualTo(10);
        assertThat(loadedEntities(Review.class))
                .as("Отзывы нужны только чтобы посчитать количество и средний рейтинг")
                .isZero();
        assertThat(sql.snapshot().roundTrips()).as("Страница + count").isLessThanOrEqualTo(2);
    }

    @Test
    void countQueryDoesNotJoinReviews() {
        resetStatistics();
        productService.catalog(0, 10);

        assertThat(sql.executedQueries())
                .filteredOn(q -> q.toLowerCase(Locale.ROOT).contains("count("))
                .as("Запрос для totalElements")
                .isNotEmpty()
                .allSatisfy(q -> assertThat(q.toLowerCase(Locale.ROOT)).doesNotContain("reviews"));
    }

    @Test
    void catalogPageIsCorrect() {
        PageDto<CatalogItemDto> page = productService.catalog(1, 10);

        Map<Long, Long> expectedReviews = jdbc.queryForList("""
                        select p.id, count(r.id) as cnt
                        from (select id from products order by id limit 10 offset 10) p
                        left join reviews r on r.product_id = p.id
                        group by p.id
                        """).stream()
                .collect(Collectors.toMap(r -> ((Number) r.get("id")).longValue(), r -> ((Number) r.get("cnt")).longValue()));
        long totalProducts = jdbc.queryForObject("select count(*) from products", Long.class);

        assertThat(page.totalElements()).isEqualTo(totalProducts);
        assertThat(page.content()).extracting(CatalogItemDto::id).containsExactlyElementsOf(
                expectedReviews.keySet().stream().sorted().toList());
        page.content().forEach(item ->
                assertThat(item.reviewsCount()).as("reviews of product %d", item.id()).isEqualTo(expectedReviews.get(item.id())));
    }
}
