package com.sqltraining.shop.web;

import com.sqltraining.shop.domain.Product;
import com.sqltraining.shop.domain.Tag;
import com.sqltraining.shop.service.ImportService;
import com.sqltraining.shop.service.ProductService;
import com.sqltraining.shop.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ImportService importService;

    @GetMapping("/catalog")
    public PageDto<CatalogItemDto> catalog(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return productService.catalog(page, size);
    }

    @GetMapping("/search")
    public List<ProductDto> search(ProductSearchFilter filter) {
        return productService.search(filter);
    }

    // CASE-13 / CASE-14
    @GetMapping("/{id}")
    public ProductDetailsDto details(@PathVariable Long id) {
        Product p = productService.getProduct(id);
        return new ProductDetailsDto(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getCategory().getName(),
                p.getTags().stream().map(Tag::getName).sorted().toList(),
                p.getReviews().stream()
                        .map(r -> new ProductDetailsDto.ReviewDto(r.getId(), r.getRating(), r.getComment(),
                                r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName(),
                                r.getCreatedAt()))
                        .toList());
    }

    @PostMapping("/reprice")
    public RepriceResultDto reprice(@RequestParam Long categoryId, @RequestParam BigDecimal percent) {
        return productService.reprice(categoryId, percent);
    }

    @PostMapping("/import")
    public ImportResultDto importProducts(@RequestParam Long categoryId,
                                          @RequestParam(defaultValue = "1000") int count) {
        return importService.importProducts(categoryId, count);
    }
}
