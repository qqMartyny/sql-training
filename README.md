# SQL Training Shop

Учебный проект для прокачки **Hibernate / Spring Data JPA / SQL / PostgreSQL**.

Это небольшой бэкенд интернет-магазина: клиенты, товары, категории, заказы, отзывы.
Архитектура обычная (controller → service → repository → БД), а вот работа с БД **намеренно плохая**:
N+1, `FetchType.EAGER`, агрегация в Java, фильтрация в памяти, нет индексов, offset-пагинация,
поштучные insert'ы, lost update, коррелированные подзапросы и т.д.

Ваша задача: пройти 14 кейсов, найти проблему, понять причину, исправить и **доказать** улучшение
(тест-задание + `EXPLAIN ANALYZE` + количество запросов в логах).

> Готовых решений в репозитории нет. В описании каждого кейса есть подсказки (спрятаны под спойлер),
> вопросы для самопроверки и ссылки на материалы.

---

## Стек

| | |
|---|---|
| Java | 21 |
| Spring Boot | 4.1 (Spring Framework 7, Spring Data JPA 4) |
| ORM | Hibernate 7.4 |
| БД | PostgreSQL 16 (+ `pg_stat_statements`) |
| Миграции | Flyway |
| Тесты | JUnit 5, Testcontainers 2, AssertJ, datasource-proxy |

## Архитектура

```
web/            REST-контроллеры и DTO (record'ы)
  └── service/        бизнес-логика, транзакции
        └── repository/   Spring Data JPA репозитории (derived queries, JPQL, native SQL)
              └── domain/      JPA-сущности
integration/    заглушка внешнего платёжного сервиса
resources/db/migration   схема (V1) и генерация данных (V2)
```

Модель данных:

```
categories (дерево через parent_id)
    └── products ──< reviews >── customers
          │  └──< product_tags >── tags         │
          └──< order_items >── orders >─────────┘
```

Объём данных по умолчанию (`seed_scale: 1`): ~20k клиентов, 5k товаров, 170 категорий,
300k заказов, ~900k позиций заказов, 60k отзывов.

## Запуск

```bash
# 1. PostgreSQL
docker compose up -d

# 2. Приложение (первый старт ~1 минута: Flyway генерирует данные)
./mvnw spring-boot:run      # или mvn spring-boot:run, или запустить ShopApplication из IDE
```

Пересоздать БД с нуля: `docker compose down -v && docker compose up -d`.

Меньше данных для быстрого старта: `mvn spring-boot:run -Dspring-boot.run.arguments=--spring.flyway.placeholders.seed_scale=0.1`
(параметр действует только при первой миграции, поэтому сначала пересоздайте БД).

> ⚠️ Некоторые эндпоинты до исправления очень медленные. `GET /api/reports/top-customers` упирается
> в `statement_timeout = 30s` (см. `application.yml`), `GET /api/reports/top-products` выполняется ~4 минуты.
> Это нормально — так и задумано. Рекомендуется сначала пройти кейс 06 (индексы).

## Тесты

```bash
mvn test                  # smoke-тесты: все эндпоинты работают. Должны быть зелёными ВСЕГДА
mvn test -Pchallenges     # задания: сейчас почти все красные, ваша цель — сделать их зелёными
mvn test -Pchallenges -Dtest=Case01NPlusOneTest   # одно задание
```

Тестам нужен Docker (Testcontainers сам поднимет PostgreSQL, данных в ~50 раз меньше).

Что есть в `src/test/java/.../support`:
- `SqlStatementCounter` — считает **реальные** обращения к БД через JDBC-прокси (round trips, select/insert/update, batch'и)
  и хранит тексты выполненных запросов (`sql.executedQueries()` — удобно распечатать при отладке);
- `IntegrationTest` — базовый класс: контейнер, `JdbcTemplate`, статистика Hibernate, HTTP-клиент;
- `ExplainHelper` — получает план запроса (`EXPLAIN`) с `enable_seqscan = off`.

**Правила игры**
1. Тесты-задания не меняем (кроме случаев, когда вы осознанно меняете API и понимаете зачем).
2. Smoke-тесты должны оставаться зелёными: оптимизация не должна ломать поведение.
3. Схему меняем **только новыми миграциями** `V3__...sql`, `V4__...sql`. `V1`/`V2` не трогаем — Flyway проверяет checksum.
4. Каждое изменение подтверждаем цифрами: было N запросов / X мс → стало M / Y мс.

## Чем смотреть, что происходит в БД

| Инструмент | Как |
|---|---|
| SQL от Hibernate | в логе приложения (`org.hibernate.SQL: debug`) |
| Значения параметров | раскомментировать `org.hibernate.orm.jdbc.bind: trace` |
| Сколько statement'ов за запрос | строка `HHH000401: Logging session metrics` в конце каждой сессии: `executing N JDBC statements`, `executing N JDBC batches` |
| План запроса | `EXPLAIN (ANALYZE, BUFFERS) <sql>` в psql / IDE. Удобно смотреть в [explain.dalibo.com](https://explain.dalibo.com) |
| Топ тяжёлых запросов | `select query, calls, mean_exec_time, rows from pg_stat_statements order by total_exec_time desc limit 20;` |
| Медленные запросы | `docker logs sql-training-db` — всё, что дольше 200 мс (`log_min_duration_statement`) |
| Подключение | `psql -h localhost -U shop shop` (пароль `shop`) или Database-панель IntelliJ IDEA |

## Кейсы

Рекомендуемый порядок — сверху вниз. Сложность: ★ — базовый, ★★ — средний, ★★★ — продвинутый.

| # | Тема | Эндпоинт | Сложность |
|---|---|---|---|
| [01](docs/cases/01-n-plus-one.md) | N+1 запросов | `GET /api/orders/recent` | ★ |
| [02](docs/cases/02-eager-fetch.md) | `FetchType.EAGER` по умолчанию | `GET /api/orders/{id}/items` | ★ |
| [03](docs/cases/03-pagination-fetch-join.md) | Пагинация + `join fetch` коллекции, count-запрос | `GET /api/products/catalog` | ★★ |
| [04](docs/cases/04-aggregation-in-java.md) | Агрегация в Java вместо SQL, проекции | `GET /api/customers/{id}/stats` | ★ |
| [05](docs/cases/05-filtering-in-memory.md) | Фильтрация в памяти, динамические запросы, поиск по подстроке | `GET /api/products/search` | ★★ |
| [06](docs/cases/06-indexes.md) | Индексы: FK, составные, функциональные | `GET /api/orders`, `GET /api/customers/by-email` | ★★ |
| [07](docs/cases/07-keyset-pagination.md) | Глубокая offset-пагинация → keyset | `GET /api/orders/feed` | ★★ |
| [08](docs/cases/08-batch-insert.md) | Batch insert, стратегии генерации id | `POST /api/products/import` | ★★ |
| [09](docs/cases/09-bulk-update.md) | Массовый update, persistence context | `POST /api/products/reprice` | ★★ |
| [10](docs/cases/10-lost-update.md) | Конкурентный доступ: lost update, блокировки | `POST /api/orders` | ★★★ |
| [11](docs/cases/11-reports.md) | Отчёты: коррелированные подзапросы, `NOT IN`, `GROUP BY`, оконные функции | `GET /api/reports/*` | ★★★ |
| [12](docs/cases/12-recursive-tree.md) | Деревья: рекурсивный CTE | `GET /api/categories/{id}/products-count` | ★★ |
| [13](docs/cases/13-open-session-in-view.md) | Open Session In View, `LazyInitializationException` | `GET /api/products/{id}` | ★★ |
| [14](docs/cases/14-multiple-bags.md) | Несколько коллекций: `MultipleBagFetchException`, декартово произведение | `GET /api/products/{id}` | ★★★ |

Дополнительные темы для самостоятельного изучения — в [docs/bonus.md](docs/bonus.md).

## Стартовые замеры (seed_scale = 1, «как есть»)

Это ваша точка отсчёта. Заполните колонку «после» своими цифрами.

| Эндпоинт | JDBC statements | Время | После |
|---|---:|---:|---|
| `GET /api/orders/recent?limit=50` | 110 | ~1.8 с | |
| `GET /api/orders/{id}/items` | 6 | ~60 мс | |
| `GET /api/products/catalog?size=20` | 288 | ~0.4 с | |
| `GET /api/customers/1/stats` | 34 | ~0.6 с | |
| `GET /api/products/search?q=phone pro&minPrice=1000` | 131 | ~0.3 с | |
| `GET /api/orders/feed?page=10000` | 22 | ~0.2 с | |
| `POST /api/products/import?count=1000` | ~2000 | ~3 с | |
| `GET /api/categories/1/products-count` | 35 | ~80 мс | |
| `GET /api/reports/monthly-revenue?year=2025` | 12 | ~0.8 с | |
| `GET /api/reports/top-customers` | 1 | > 30 с (timeout) | |
| `GET /api/reports/top-products?n=3` | ~5000 | ~4 мин | |

Цифры получены на конкретной машине. У вас могут отличаться, важен порядок величин.

## Как это оформить в резюме

Не «знаю Hibernate», а конкретика с цифрами, например:
- «Устранил N+1 в выдаче заказов: 110 → 2 SQL-запроса, p95 с 1.8 с до 40 мс (entity graph, batch fetching)»;
- «Спроектировал индексы под реальные запросы (составные, функциональные, GIN pg_trgm), подтверждал планами `EXPLAIN ANALYZE`»;
- «Переписал отчёты с коррелированных подзапросов на `GROUP BY` и оконные функции: с >30 с до 200 мс»;
- «Устранил lost update при списании остатков (атомарный `UPDATE`, optimistic locking с retry), покрыл конкурентным тестом».

Хорошая практика — вести `docs/solutions.md` (или PR на каждый кейс): что было, почему плохо, что сделали, цифры до/после.
Это готовый материал для собеседования.
