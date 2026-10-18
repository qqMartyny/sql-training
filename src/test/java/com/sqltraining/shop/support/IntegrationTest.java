package com.sqltraining.shop.support;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Базовый класс интеграционных тестов.
 * <p>
 * Один контейнер PostgreSQL на весь прогон (singleton container pattern), схема и данные — из Flyway
 * (уменьшенный объём, см. src/test/resources/application-test.yml).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestDatabaseConfiguration.class)
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected SqlStatementCounter sql;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Value("${local.server.port}")
    private int port;

    @BeforeEach
    void resetCounters() {
        resetStatistics();
    }

    /** Сбросить счётчики SQL и статистику Hibernate. Вызывайте прямо перед измеряемым действием. */
    protected void resetStatistics() {
        sql.reset();
        hibernateStatistics().clear();
    }

    /** Статистика Hibernate: сколько сущностей загружено, сколько коллекций инициализировано и т.д. */
    protected Statistics hibernateStatistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    protected long loadedEntities(Class<?> entityClass) {
        return hibernateStatistics().getEntityStatistics(entityClass.getName()).getLoadCount();
    }

    protected RestClient http() {
        return RestClient.create("http://localhost:" + port);
    }
}
