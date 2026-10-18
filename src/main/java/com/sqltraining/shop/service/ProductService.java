package com.sqltraining.shop.service;

import com.sqltraining.shop.domain.Product;
import com.sqltraining.shop.domain.Review;
import com.sqltraining.shop.repository.ProductRepository;
import com.sqltraining.shop.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * CASE-03. Страница каталога: товар, категория, количество отзывов и средний рейтинг.
     */
    @Transactional
    public PageDto<CatalogItemDto> catalog(int page, int size) {
        Page<Product> products = productRepository.findAllWithReviews(PageRequest.of(page, size, Sort.by("id")));
        return PageDto.of(products.map(p -> new CatalogItemDto(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getCategory().getName(),
                p.getReviews().size(),
                p.getReviews().stream().mapToInt(Review::getRating).average().stream().boxed().findFirst().orElse(null))));
    }

    /**
     * CASE-05. Поиск товаров по фильтру.
     */
    @Transactional
    public List<ProductDto> search(ProductSearchFilter filter) {
        return productRepository.findAll().stream()
                .filter(p -> filter.q() == null
                        || p.getName().toLowerCase(Locale.ROOT).contains(filter.q().toLowerCase(Locale.ROOT)))
                .filter(p -> filter.minPrice() == null || p.getPrice().compareTo(filter.minPrice()) >= 0)
                .filter(p -> filter.maxPrice() == null || p.getPrice().compareTo(filter.maxPrice()) <= 0)
                .filter(p -> filter.categoryId() == null || p.getCategory().getId().equals(filter.categoryId()))
                .filter(p -> filter.inStock() == null || !filter.inStock() || p.getStock() > 0)
                .map(p -> new ProductDto(p.getId(), p.getSku(), p.getName(), p.getPrice(), p.getStock(),
                        p.getCategory().getId()))
                .toList();
    }

    /**
     * CASE-13 / CASE-14. Карточка товара. Маппинг в DTO делает контроллер.
     */
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }

    /**
     * CASE-09. Изменить цены всех товаров категории на percent процентов.
     */
    @Transactional
    public RepriceResultDto reprice(Long categoryId, BigDecimal percent) {
        BigDecimal factor = BigDecimal.ONE.add(percent.movePointLeft(2));
        List<Product> products = productRepository.findByCategoryId(categoryId);
        for (Product p : products) {
            p.setPrice(p.getPrice().multiply(factor).setScale(2, RoundingMode.HALF_UP));
        }
        return new RepriceResultDto(products.size());
    }
}
