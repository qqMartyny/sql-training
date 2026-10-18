package com.sqltraining.shop.repository;

import com.sqltraining.shop.domain.Customer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Отчёты на нативном SQL. Результаты маппятся в интерфейсные проекции.
 */
public interface ReportRepository extends Repository<Customer, Long> {

    interface TopCustomerRow {
        Long getId();

        String getEmail();

        Long getOrdersCount();

        BigDecimal getTotalSpent();
    }

    interface InactiveCustomerRow {
        Long getId();

        String getEmail();

        Instant getRegisteredAt();
    }

    // CASE-11
    @Query(nativeQuery = true, value = """
            select c.id,
                   c.email,
                   (select count(*) from orders o where o.customer_id = c.id) as "ordersCount",
                   (select coalesce(sum(oi.price * oi.quantity), 0)
                      from orders o
                      join order_items oi on oi.order_id = o.id
                     where o.customer_id = c.id
                       and o.status <> 'CANCELLED') as "totalSpent"
              from customers c
             order by "totalSpent" desc
             limit :limit
            """)
    List<TopCustomerRow> findTopCustomers(@Param("limit") int limit);

    // CASE-11
    @Query(nativeQuery = true, value = """
            select c.id, c.email, c.registered_at as "registeredAt"
              from customers c
             where c.id not in (select o.customer_id from orders o where o.created_at >= :since)
             order by c.id
            """)
    List<InactiveCustomerRow> findInactiveCustomers(@Param("since") Instant since);
}
