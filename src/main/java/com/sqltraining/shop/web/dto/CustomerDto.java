package com.sqltraining.shop.web.dto;

public record CustomerDto(Long id, String email, String firstName, String lastName, String city) {
}
