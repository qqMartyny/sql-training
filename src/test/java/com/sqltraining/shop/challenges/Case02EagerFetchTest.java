package com.sqltraining.shop.challenges;

import com.sqltraining.shop.service.OrderService;
import com.sqltraining.shop.support.IntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("challenge")
class Case02EagerFetchTest extends IntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void loadingOrderItemsDoesNotPullHalfOfTheDatabase() {
        Long orderId = jdbc.queryForObject(
                "select order_id from order_items group by order_id order by count(*) desc, order_id limit 1", Long.class);

        resetStatistics();
        var items = orderService.findItems(orderId);

        assertThat(items).isNotEmpty();
        assertThat(sql.snapshot().roundTrips())
                .as("Для списка позиций заказа нужен ровно один SELECT")
                .isEqualTo(1);
    }

    @Test
    void allToOneAssociationsAreLazy() {
        List<String> eager = new ArrayList<>();
        for (EntityType<?> type : entityManagerFactory.getMetamodel().getEntities()) {
            Class<?> entity = type.getJavaType();
            for (Field field : entity.getDeclaredFields()) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                FetchType fetch = manyToOne != null ? manyToOne.fetch() : oneToOne != null ? oneToOne.fetch() : null;
                if (fetch == FetchType.EAGER) {
                    eager.add(entity.getSimpleName() + "." + field.getName());
                }
            }
        }
        assertThat(eager).as("Ассоциации с FetchType.EAGER (явно или по умолчанию)").isEmpty();
    }
}
