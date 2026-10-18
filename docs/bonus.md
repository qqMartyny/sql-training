# Бонус: темы для самостоятельного изучения

Эти темы не покрыты тестами — придумайте сценарий в этом же проекте и проверьте сами.

## Hibernate / JPA

- **Кеш второго уровня** (Hibernate + JCache/Caffeine/Ehcache). Закешируйте `Category` и `Tag` — справочники,
  которые почти не меняются. Что такое query cache и почему он часто вредит?
- **Lombok и сущности.** Добавьте `@ToString` на `Order` и залогируйте заказ. Что произойдёт? Почему `@Data`
  на сущности — плохая идея?
- **`equals`/`hashCode`** для сущностей: по id, по natural id (`@NaturalId`), почему не по всем полям.
- **Bytecode enhancement**: lazy-загрузка отдельных полей (`@Basic(fetch = LAZY)` для `description`), dirty tracking.
- **`@Formula`, `@Subselect`, `@Immutable`** — когда сущность лучше построить поверх SQL-представления.
- **Soft delete** (`@SoftDelete` в Hibernate 6.4+) и его влияние на индексы (частичные индексы).
- **Аудит**: Hibernate Envers vs Spring Data `@CreatedDate`/`@LastModifiedDate`.
- **`StatelessSession`** для массовой обработки.

## Spring

- **`@Transactional`**: propagation (`REQUIRES_NEW`, `NESTED`), self-invocation, `rollbackFor`,
  `TransactionTemplate`, `@TransactionalEventListener`.
- **Hikari**: размер пула, `connection-timeout`, `leak-detection-threshold`. Воспроизведите исчерпание пула
  через кейс 10 (`PaymentGateway` внутри транзакции) и найдите это в метриках.
- **Actuator + Micrometer**: метрики Hikari и Hibernate, `/actuator/metrics/hikaricp.connections.active`.
- **Read replica**: роутинг `readOnly`-транзакций на реплику (`AbstractRoutingDataSource`).

## PostgreSQL

- **`pg_stat_statements`**: найдите топ-5 запросов по `total_exec_time` после прогона всех эндпоинтов.
- **`auto_explain`**: логирование планов медленных запросов прямо в лог БД.
- **VACUUM, autovacuum, bloat**: что происходит с таблицей после кейса 09 (массовые update)? `pg_stat_user_tables.n_dead_tup`.
- **Партиционирование** `orders` по `created_at` (декларативное, по месяцам). Как это влияет на запросы и индексы?
- **Материализованные представления** для отчётов из кейса 11. `refresh materialized view concurrently`.
- **JSONB** + GIN-индекс: атрибуты товара (цвет, размер) в `jsonb`.
- **Блокировки и deadlock'и**: `pg_locks`, `pg_stat_activity`, `lock_timeout`.

## Архитектура

- **CQRS-lite**: отдельные read-модели (проекции) для чтения и сущности для записи.
- **Outbox pattern**: как надёжно отправить событие «заказ создан» в Kafka вместе с коммитом транзакции.
- **Идемпотентность** `POST /api/orders` (ключ идемпотентности + unique-ограничение).
- **jOOQ** как альтернатива/дополнение JPA для сложных отчётов.
