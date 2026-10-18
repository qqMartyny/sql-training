package com.sqltraining.shop.web;

import com.sqltraining.shop.repository.ReportRepository;
import com.sqltraining.shop.service.ReportService;
import com.sqltraining.shop.web.dto.MonthlyRevenueDto;
import com.sqltraining.shop.web.dto.TopProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/top-customers")
    public List<ReportRepository.TopCustomerRow> topCustomers(@RequestParam(defaultValue = "10") int limit) {
        return reportService.topCustomers(limit);
    }

    @GetMapping("/inactive-customers")
    public List<ReportRepository.InactiveCustomerRow> inactiveCustomers(@RequestParam(defaultValue = "180") int days) {
        return reportService.inactiveCustomers(days);
    }

    @GetMapping("/monthly-revenue")
    public List<MonthlyRevenueDto> monthlyRevenue(@RequestParam int year) {
        return reportService.monthlyRevenue(year);
    }

    @GetMapping("/top-products")
    public List<TopProductDto> topProducts(@RequestParam(defaultValue = "3") int n) {
        return reportService.topProductsPerCategory(n);
    }
}
