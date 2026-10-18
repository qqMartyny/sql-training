# Кейс 08. Batch insert и генерация идентификаторов

**Эндпоинт:** `POST /api/products/import?categoryId=51&count=1000`
**Код:** `ImportService.importProducts`, `Product.id`
**Тест:** `Case08BatchInsertTest`

## Ситуация

Каждую ночь импортируется прайс поставщика — десятки тысяч товаров. 1000 товаров импортируются ~3 секунды,
100 000 — несколько минут. При этом в логах ровно ~2000 запросов на 1000 товаров.

## Как воспроизвести

```bash
curl -X POST "http://localhost:8080/api/products/import?categoryId=51&count=1000"
```

Смотрите в `session metrics`: сколько сессий открылось, сколько `JDBC statements` и `JDBC batches`.

## Задание

1. Объясните все причины, почему каждый товар — это отдельные обращения к БД. Их несколько:
   транзакции, повторное чтение категории, стратегия генерации `id`, настройки batching.
2. Сделайте так, чтобы insert'ы уходили пачками. Тест требует ≤ 30 round trip'ов на 500 товаров.
3. Замерьте время импорта 10 000 товаров до и после.
4. Бонус: попробуйте `reWriteBatchedInserts=true` в JDBC URL PostgreSQL. Что он делает? Видно ли это в счётчике round trip'ов?
5. Бонус: сравните с `JdbcTemplate.batchUpdate` и с `COPY`. Когда ORM — не лучший инструмент?

**Готово, когда:** `Case08BatchInsertTest` зелёный.

## Подсказки

<details><summary>Подсказка 1 — транзакция</summary>

Что делает `save()`, когда вызывается вне транзакции? Сколько раз здесь открывается persistence context?
Что произойдёт с `findById(categoryId)`, если весь импорт пойдёт в одной транзакции?
</details>

<details><summary>Подсказка 2 — IDENTITY</summary>

При `GenerationType.IDENTITY` Hibernate обязан выполнить `insert` сразу при `persist`, чтобы узнать `id`.
Поэтому JDBC batching для insert'ов с IDENTITY отключается. Альтернатива — `SEQUENCE` c `allocationSize`
(и оптимизатор `pooled`). Не забудьте, что sequence нужно создать миграцией и выставить её стартовое значение
больше текущего `max(id)`.
</details>

<details><summary>Подсказка 3 — настройки</summary>

`spring.jpa.properties.hibernate.jdbc.batch_size`, `hibernate.order_inserts`, `hibernate.order_updates`.
Для очень больших импортов — периодический `flush()` + `clear()`. Зачем `clear()`?
</details>

<details><summary>Подсказка 4 — ссылка без запроса</summary>

Чтобы проставить связь, сама сущность категории не нужна — достаточно ссылки. Посмотрите `getReferenceById`
(`EntityManager.getReference`).
</details>

## Вопросы для самопроверки

- Чем `save()` отличается от `persist()` и `merge()`? Когда `save()` делает лишний `select`?
- Что такое `allocationSize` и что будет, если он не совпадает с `increment by` у sequence?
- Почему `UUID` v4 в качестве первичного ключа плох для B-tree индекса, а UUID v7 — нет?
- Почему Hibernate не может батчить insert'ы с разными таблицами без `order_inserts`?

## Материалы

- Vlad Mihalcea — *How to batch INSERT and UPDATE statements with Hibernate*
- Vlad Mihalcea — *Why you should never use the TABLE identifier generator* / *Hibernate pooled optimizer*
- Hibernate User Guide — *Batching*, *Identifier generators*
