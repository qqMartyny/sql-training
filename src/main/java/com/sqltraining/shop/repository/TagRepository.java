package com.sqltraining.shop.repository;

import com.sqltraining.shop.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
