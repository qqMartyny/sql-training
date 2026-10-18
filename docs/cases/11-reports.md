# Кейс 11. Отчёты на SQL

**Эндпоинты:**
- `GET /api/reports/top-customers?limit=10` — топ клиентов по сумме покупок
- `GET /api/reports/inactive-customers?days=180` — клиенты без заказов за период
- `GET /api/reports/monthly-revenue?year=2025` — выручка по месяцам
- `GET /api/reports/top-products?n=3` — топ-N товаров в каждой категории

**Код:** `ReportService`, `ReportRepository` (native SQL), `OrderRepository.sumRevenue`, `OrderItemRepository.sumSoldQuantity`
**Тест:** `Case11ReportsTest`

> Пройдите сначала кейс 06. Без индексов `top-customers` падает по `statement_timeout`, а `top-products` выполняется ~4 минуты.

## 11.1. Топ клиентов — коррелированные подзапросы

Для **каждого** клиента выполняются два подзапроса, затем вся таблица сортируется по вычисленному полю.
Посмотрите `explain` — найдите `SubPlan`. Сколько раз он выполняется (`loops` в `explain analyze`)?

**Задание:** перепишите на `join` + `group by` (или CTE / подзапрос в `from`). В плане не должно быть `SubPlan`.
Подумайте, нужно ли вообще джойнить `customers` до агрегации или после. Сравните варианты по `explain analyze`.

## 11.2. Неактивные клиенты — NOT IN

**Задание:**
1. Разберитесь, почему `not in (subquery)` — опасная конструкция. Что вернёт запрос, если в подзапросе окажется `NULL`?
   (Здесь `customer_id not null`, но в реальной жизни — не всегда.)
2. Перепишите на `not exists` и найдите в плане `Anti Join`. Сравните с вариантом `left join ... where o.id is null`.

## 11.3. Выручка по месяцам — 12 запросов вместо одного

**Задание:** один запрос с группировкой по месяцу. Месяцы без продаж должны остаться в ответе с нулём.
Где удобнее «дозаполнить» пустые месяцы — в SQL (`generate_series`) или в Java? Обоснуйте.
Учтите таймзону: границы месяца считаются в UTC.

## 11.4. Топ-N товаров в каждой категории — классика «greatest-n-per-group»

Сейчас: цикл по категориям → цикл по товарам → запрос на каждый товар. Тысячи запросов.

**Задание:** один SQL-запрос. Порядок: по проданному количеству по убыванию, при равенстве — по `id` товара.
Товары без продаж участвуют с нулём. Учитываются только листовые категории (без дочерних).

## Готово, когда

`Case11ReportsTest` зелёный: все 8 тестов (план без `SubPlan`, `Anti Join`, по одному запросу, корректные результаты).

## Подсказки

<details><summary>11.1</summary>

Сначала посчитайте агрегаты по `orders`/`order_items` с `group by customer_id`, потом присоедините клиентов.
`count(distinct o.id)` vs отдельный подсчёт: что дешевле? Условная агрегация: `sum(...) filter (where ...)`.
</details>

<details><summary>11.3</summary>

`date_trunc('month', created_at at time zone 'UTC')` или `extract(month from ...)`. Как это повлияет на использование индекса по `created_at`?
Почему фильтр по году лучше писать как диапазон `created_at >= :from and created_at < :to`, а не `extract(year ...) = :year`?
</details>

<details><summary>11.4</summary>

Оконные функции: `row_number() over (partition by ... order by ...)`. Отфильтровать по номеру строки можно
только во внешнем запросе (почему нельзя в `where` того же уровня?). Альтернатива — `lateral join` с `limit`.
Сравните планы двух вариантов.
</details>

## Вопросы для самопроверки

- Чем `row_number`, `rank` и `dense_rank` отличаются? Какой выбрать для «топ-3» при равенстве продаж?
- Что такое CTE и когда в PostgreSQL 12+ он материализуется (`materialized` / `not materialized`)?
- `exists` vs `in` vs `join`: когда планировщик превращает их в одно и то же, а когда нет?
- Как маппить результат native-запроса: интерфейсная проекция, `@SqlResultSetMapping`, `Tuple`, DTO-конструктор?
- Где должны жить тяжёлые отчёты: в OLTP-базе, на реплике, в материализованном представлении, в OLAP-хранилище?

## Материалы

- PostgreSQL docs — *Window Functions*, *WITH Queries*, *Subquery Expressions*
- Markus Winand — *modern-sql.com*
- Postgres Professional — курс «QPT. Оптимизация запросов» (бесплатно)
