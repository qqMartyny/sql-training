package com.sqltraining.shop.repository;

import com.sqltraining.shop.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
