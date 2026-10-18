package com.sqltraining.shop.support;

import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Оборачивает DataSource приложения в прокси, который считает SQL-запросы.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfiguration {

    public static final SqlStatementCounter COUNTER = new SqlStatementCounter();

    @Bean
    SqlStatementCounter sqlStatementCounter() {
        return COUNTER;
    }

    @Bean
    static BeanPostProcessor countingDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof ProxyDataSource)) {
                    return ProxyDataSourceBuilder.create(dataSource)
                            .name("counting")
                            .listener(COUNTER)
                            .build();
                }
                return bean;
            }
        };
    }
}
