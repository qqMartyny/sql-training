package com.sqltraining.shop.repository;

import com.sqltraining.shop.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    // CASE-03
    @Query("select distinct p from Product p left join fetch p.reviews")
    Page<Product> findAllWithReviews(Pageable pageable);
}
