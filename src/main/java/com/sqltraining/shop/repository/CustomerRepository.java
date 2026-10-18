package com.sqltraining.shop.repository;

import com.sqltraining.shop.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // CASE-06: посмотрите, какой SQL генерирует Spring Data для IgnoreCase
    Optional<Customer> findByEmailIgnoreCase(String email);
}
