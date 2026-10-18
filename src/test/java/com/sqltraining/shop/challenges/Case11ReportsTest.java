package com.sqltraining.shop.challenges;

import com.sqltraining.shop.repository.ReportRepository;
import com.sqltraining.shop.service.ReportService;
import com.sqltraining.shop.support.ExplainHelper;
import com.sqltraining.shop.support.IntegrationTest;
import com.sqltraining.shop.web.dto.MonthlyRevenueDto;
import com.sqltraining.shop.web.dto.TopProductDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Эталонные значения считаются в Java по «сырым» данным — так тест не подсказывает решение.
 */
@Tag("challenge")
class Case11ReportsTest extends IntegrationTest {

    @Autowired
    ReportService reportService;

    @Autowired
    TransactionTemplate tx;

    // ---------- top customers ----------

    @Test
    void topCustomersQueryHasNoCorrelatedSubqueries() throws Exception {
        String plan = new ExplainHelper(jdbc, tx).explain(nativeSql("findTopCustomers"), Map.of("limit", "10"), false);

        assertThat(plan).as(plan).doesNotContain("SubPlan");
    }

    @Test
    void topCustomersAreCorrect() {
        Map<Long, BigDecimal> spent = new HashMap<>();
        jdbc.query("""
                select o.customer_id, oi.price * oi.quantity as amount
                from orders o join order_items oi on oi.order_id = o.id where o.status <> 'CANCELLED'
                """, rs -> {
            spent.merge(rs.getLong(1), rs.getBigDecimal(2), BigDecimal::add);
        });
        List<BigDecimal> expectedTop = spent.values().stream().sorted(Comparator.reverseOrder()).limit(10).toList();

        List<ReportRepository.TopCustomerRow> top = reportService.topCustomers(10);

        assertThat(top).extracting(ReportRepository.TopCustomerRow::getTotalSpent)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyElementsOf(expectedTop);
        top.forEach(row -> assertThat(row.getOrdersCount()).isEqualTo(
                jdbc.queryForObject("select count(*) from orders where customer_id = ?", Long.class, row.getId())));
    }

    // ---------- inactive customers ----------

    @Test
    void inactiveCustomersQueryUsesAntiJoin() throws Exception {
        String plan = new ExplainHelper(jdbc, tx).explain(
                nativeSql("findInactiveCustomers"), Map.of("since", "now() - interval '90 days'"), false);

        assertThat(plan).as(plan).doesNotContain("SubPlan").contains("Anti Join");
    }

    @Test
    void inactiveCustomersAreCorrect() {
        Set<Long> active = new HashSet<>(jdbc.queryForList(
                "select customer_id from orders where created_at >= now() - interval '90 days'", Long.class));
        List<Long> expected = jdbc.queryForList("select id from customers order by id", Long.class).stream()
                .filter(id -> !active.contains(id))
                .toList();

        assertThat(reportService.inactiveCustomers(90))
                .extracting(ReportRepository.InactiveCustomerRow::getId)
                .containsExactlyElementsOf(expected);
    }

    // ---------- monthly revenue ----------

    @Test
    void monthlyRevenueIsOneQuery() {
        resetStatistics();
        reportService.monthlyRevenue(Year.now(ZoneOffset.UTC).getValue());

        assertThat(sql.snapshot().roundTrips()).isEqualTo(1);
    }

    @Test
    void monthlyRevenueIsCorrect() {
        int year = Year.now(ZoneOffset.UTC).getValue() - 1;
        BigDecimal[] expected = new BigDecimal[13];
        Arrays.fill(expected, BigDecimal.ZERO);
        jdbc.query("""
                select o.created_at, oi.price * oi.quantity
                from orders o join order_items oi on oi.order_id = o.id where o.status <> 'CANCELLED'
                """, rs -> {
            LocalDateTime at = rs.getObject(1, OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            if (at.getYear() == year) {
                expected[at.getMonthValue()] = expected[at.getMonthValue()].add(rs.getBigDecimal(2));
            }
        });

        List<MonthlyRevenueDto> report = reportService.monthlyRevenue(year);

        assertThat(report).hasSize(12);
        report.forEach(m -> assertThat(m.revenue()).as("month %d", m.month()).isEqualByComparingTo(expected[m.month()]));
    }

    // ---------- top products per category ----------

    @Test
    void topProductsPerCategoryIsOneQuery() {
        resetStatistics();
        reportService.topProductsPerCategory(3);

        assertThat(sql.snapshot().roundTrips()).isEqualTo(1);
    }

    @Test
    void topProductsPerCategoryAreCorrect() {
        Map<Long, Long> soldByProduct = new HashMap<>();
        jdbc.query("select product_id, quantity from order_items",
                rs -> {
                    soldByProduct.merge(rs.getLong(1), rs.getLong(2), Long::sum);
                });
        Set<Long> parents = new HashSet<>(jdbc.queryForList(
                "select parent_id from categories where parent_id is not null", Long.class));
        Map<Long, List<Long>> productsByLeaf = new TreeMap<>();
        jdbc.query("select id, category_id from products", rs -> {
            if (!parents.contains(rs.getLong(2))) {
                productsByLeaf.computeIfAbsent(rs.getLong(2), k -> new ArrayList<>()).add(rs.getLong(1));
            }
        });
        Map<Long, List<Long>> expected = new TreeMap<>();
        productsByLeaf.forEach((category, products) -> expected.put(category, products.stream()
                .sorted(Comparator.<Long>comparingLong(p -> soldByProduct.getOrDefault(p, 0L)).reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .limit(3)
                .toList()));

        Map<Long, List<Long>> actual = reportService.topProductsPerCategory(3).stream()
                .collect(Collectors.groupingBy(TopProductDto::categoryId, TreeMap::new,
                        Collectors.mapping(TopProductDto::productId, Collectors.toList())));

        assertThat(actual).isEqualTo(expected);
    }

    private static String nativeSql(String method) throws NoSuchMethodException {
        return Arrays.stream(ReportRepository.class.getMethods())
                .filter(m -> m.getName().equals(method))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(method))
                .getAnnotation(Query.class)
                .value();
    }
}
