package com.sqltraining.shop.challenges;

import com.sqltraining.shop.support.ExplainHelper;
import com.sqltraining.shop.support.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Запросы ниже — по форме ровно то, что генерирует Hibernate для методов репозиториев
 * (проверьте это в логах приложения).
 */
@Tag("challenge")
@SuppressWarnings("SqlSourceToSinkFlow")
class Case06IndexesTest extends IntegrationTest {

    @Autowired
    TransactionTemplate tx;

    private ExplainHelper explain() {
        return new ExplainHelper(jdbc, tx);
    }

    @Test
    void ordersByStatusAndPeriodUseIndexWithoutSorting() {
        // OrderRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc
        String plan = explain().explain("""
                select * from orders o
                where o.status = 'NEW' and o.created_at between now() - interval '1 day' and now()
                order by o.created_at desc
                """, Map.of(), true);

        assertThat(plan).as(plan).doesNotContain("Seq Scan on orders");
        assertThat(plan).as("Индекс должен отдавать строки уже в нужном порядке\n" + plan).doesNotContain("Sort");
    }

    @Test
    void customerLookupByEmailIgnoringCaseUsesIndex() {
        // CustomerRepository.findByEmailIgnoreCase
        String plan = explain().explain(
                "select * from customers c where upper(c.email) = upper('user42@mail.example')", Map.of(), true);

        assertThat(plan).as(plan).doesNotContain("Seq Scan on customers");
    }

    @Test
    void everyForeignKeyIsCoveredByIndex() {
        List<String> notIndexed = jdbc.queryForList("""
                select c.conrelid::regclass || '(' || a.attname || ')'
                from pg_constraint c
                join pg_attribute a on a.attrelid = c.conrelid and a.attnum = c.conkey[1]
                where c.contype = 'f'
                  and not exists (select 1
                                  from pg_index i
                                  where i.indrelid = c.conrelid
                                    and i.indkey[0] = c.conkey[1])
                order by 1
                """, String.class);

        assertThat(notIndexed).as("Внешние ключи, по которым нет индекса (первой колонкой)").isEmpty();
    }
}
