package com.sqltraining.shop.service;

import com.sqltraining.shop.domain.Category;
import com.sqltraining.shop.domain.Product;
import com.sqltraining.shop.repository.*;
import com.sqltraining.shop.web.dto.MonthlyRevenueDto;
import com.sqltraining.shop.web.dto.TopProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * CASE-11. Топ клиентов по сумме покупок (без отменённых заказов).
     */
    @Transactional
    public List<ReportRepository.TopCustomerRow> topCustomers(int limit) {
        return reportRepository.findTopCustomers(limit);
    }

    /**
     * CASE-11. Клиенты без единого заказа за последние days дней.
     */
    @Transactional
    public List<ReportRepository.InactiveCustomerRow> inactiveCustomers(int days) {
        return reportRepository.findInactiveCustomers(Instant.now().minus(Duration.ofDays(days)));
    }

    /**
     * CASE-11. Выручка по месяцам заданного года (UTC, без отменённых заказов).
     */
    @Transactional
    public List<MonthlyRevenueDto> monthlyRevenue(int year) {
        List<MonthlyRevenueDto> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            Instant from = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            result.add(new MonthlyRevenueDto(month, orderRepository.sumRevenue(from, to)));
        }
        return result;
    }

    /**
     * CASE-11. Топ-N самых продаваемых (по количеству единиц) товаров в каждой листовой категории.
     * При равенстве продаж выше товар с меньшим id.
     */
    @Transactional
    public List<TopProductDto> topProductsPerCategory(int n) {
        List<TopProductDto> result = new ArrayList<>();
        for (Category category : categoryRepository.findAll()) {
            if (!category.getChildren().isEmpty()) {
                continue;
            }
            record Sold(Product product, long quantity) {
            }
            productRepository.findByCategoryId(category.getId()).stream()
                    .map(p -> new Sold(p, orderItemRepository.sumSoldQuantity(p.getId())))
                    .sorted(Comparator.comparingLong(Sold::quantity).reversed()
                            .thenComparing(s -> s.product().getId()))
                    .limit(n)
                    .forEach(s -> result.add(new TopProductDto(category.getId(), category.getName(),
                            s.product().getId(), s.product().getName(), s.quantity())));
        }
        return result;
    }
}
