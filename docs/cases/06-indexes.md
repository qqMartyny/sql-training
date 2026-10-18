# Кейс 06. Индексы

**Эндпоинты:**
- `GET /api/orders?status=NEW&from=2026-10-01T00:00:00Z&to=2026-10-18T00:00:00Z`
- `GET /api/customers/by-email?email=user42@mail.example`
- и косвенно — почти все остальные

**Код:** `OrderRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc`, `CustomerRepository.findByEmailIgnoreCase`, `V1__schema.sql`
**Тест:** `Case06IndexesTest`

## Ситуация

В схеме есть только первичные ключи и `unique`-ограничения. Индексов на внешних ключах нет
(PostgreSQL, в отличие от MySQL, **не создаёт их автоматически**). Под фильтры и сортировки индексов тоже нет.

## Как воспроизвести

1. Включите логирование параметров или возьмите SQL из лога.
2. Выполните в psql:

```sql
explain (analyze, buffers)
select * from orders o
where o.status = 'NEW' and o.created_at between now() - interval '7 days' and now()
order by o.created_at desc;

explain (analyze, buffers)
select * from customers c where upper(c.email) = upper('user42@mail.example');
```

3. Найдите в плане `Seq Scan`, `Sort`, `Rows Removed by Filter`, `Buffers: shared read/hit`.

## Задание

1. Создайте миграцию `V3__indexes.sql`.
2. **Заказы по статусу и периоду.** Подберите индекс так, чтобы не было ни `Seq Scan`, ни `Sort`.
   Объясните, почему порядок колонок в составном индексе важен. Попробуйте оба порядка и сравните планы.
   Бонус: частичный индекс (`where status = 'NEW'`) — когда он уместен?
3. **Email без учёта регистра.** Обратите внимание, какую функцию генерирует Spring Data для `IgnoreCase`.
   Почему существующий `unique`-индекс по `email` не помогает?
4. **Внешние ключи.** Проиндексируйте FK-колонки. Объясните, зачем индекс на FK нужен не только для `join`, но и
   для `delete` из родительской таблицы.
5. Проверьте, как изменилось время других эндпоинтов (`/recent`, `/stats`, отчёты). Заполните таблицу в README.

**Готово, когда:** `Case06IndexesTest` зелёный.

## Подсказки

<details><summary>Подсказка 1 — составной индекс</summary>

Правило: сначала колонки с условием равенства, потом — колонка диапазона/сортировки.
Посмотрите, что B-tree умеет читаться в обратном порядке (`Index Scan Backward`).
</details>

<details><summary>Подсказка 2 — функциональный индекс</summary>

Индекс может быть построен по выражению: `create index ... on customers (upper(email))`.
Выражение в запросе должно совпадать с выражением в индексе **буквально**. `lower` ≠ `upper`.
</details>

<details><summary>Подсказка 3 — тестовая БД маленькая</summary>

На маленьких таблицах PostgreSQL честно предпочитает `Seq Scan` — это дешевле. Поэтому тест строит план
с `enable_seqscan = off`: проверяется, что индекс **есть и применим**. На большой базе проверяйте
через `EXPLAIN ANALYZE`.
</details>

<details><summary>Подсказка 4 — создание индекса на живой базе</summary>

`create index` блокирует запись в таблицу. В продакшене используют `create index concurrently`,
но его нельзя выполнить внутри транзакции. Как это сделать во Flyway? (поищите `executeInTransaction`
и конфигурацию скриптов `.conf`).
</details>

## Вопросы для самопроверки

- Как устроен B-tree индекс? Почему он ускоряет `=`, `<`, `between`, `order by`, но не `like '%x%'`?
- Что такое index-only scan и covering index (`include`)? Что такое visibility map?
- Почему индекс может не использоваться, даже если он есть? (статистика, селективность, приведение типов, функции)
- Чем индексы мешают? (запись, место, HOT-updates)
- Как найти неиспользуемые индексы? (`pg_stat_user_indexes`)

## Материалы

- Markus Winand — *Use The Index, Luke!* (обязательно)
- PostgreSQL docs — *Indexes*, *Using EXPLAIN*
- Егор Рогов — серия статей «Индексы в PostgreSQL» (Хабр, Postgres Professional)
