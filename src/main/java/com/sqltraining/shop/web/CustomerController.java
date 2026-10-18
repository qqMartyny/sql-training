package com.sqltraining.shop.web;

import com.sqltraining.shop.service.CustomerService;
import com.sqltraining.shop.web.dto.CustomerDto;
import com.sqltraining.shop.web.dto.CustomerStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/by-email")
    public CustomerDto byEmail(@RequestParam String email) {
        return customerService.findByEmail(email);
    }

    @GetMapping("/{id}/stats")
    public CustomerStatsDto stats(@PathVariable Long id) {
        return customerService.getStats(id);
    }
}
