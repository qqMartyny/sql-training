# Кейс 02. FetchType.EAGER по умолчанию

**Эндпоинт:** `GET /api/orders/{id}/items`
**Код:** все сущности в `domain/`, `OrderService.findItems`
**Тест:** `Case02EagerFetchTest`

## Ситуация

Эндпоинт возвращает позиции одного заказа: id позиции, id товара, количество, цену.
Всё, что нужно, лежит в одной таблице `order_items`. Но в логах — 6+ запросов, включая `customers`,
`categories` и даже родительские категории.

## Как воспроизвести

```bash
curl "http://localhost:8080/api/orders/1/items"
```

Посмотрите, какие таблицы участвуют в запросах. Откройте сущности `OrderItem`, `Product`, `Category`, `Order`, `Review`.

## Задание

1. Найдите все ассоциации, которые загружаются жадно. Обратите внимание: в коде нигде не написано `EAGER`.
2. Переведите связи на `LAZY` и убедитесь, что ничего не сломалось (smoke-тесты!).
3. Объясните, почему `order.getCustomer().getId()` на lazy-прокси не делает запрос, а `getEmail()` — делает.

**Готово, когда:** `Case02EagerFetchTest` зелёный: ни одной `EAGER` to-one связи, позиции заказа — ровно 1 запрос.

## Подсказки

<details><summary>Подсказка 1</summary>

Посмотрите значение по умолчанию атрибута `fetch` в аннотациях `@ManyToOne`, `@OneToOne`, `@OneToMany`, `@ManyToMany`.
</details>

<details><summary>Подсказка 2</summary>

EAGER нельзя «выключить» в конкретном запросе. LAZY можно «включить» там, где нужно (fetch join, entity graph).
Поэтому общее правило — всё LAZY, а нужное — догружаем явно под конкретный сценарий.
</details>

<details><summary>Подсказка 3 — что может сломаться</summary>

После перехода на LAZY в некоторых местах может появиться `LazyInitializationException` или новый N+1.
Это не повод вернуть EAGER — это повод разобраться с границами транзакций (кейс 13) и fetch-планами (кейсы 01, 14).
</details>

## Вопросы для самопроверки

- Почему `findById` с EAGER-связями делает один запрос с join'ами, а JPQL-запрос — несколько запросов?
- Что такое Hibernate-прокси? Как он устроен (подкласс, bytecode)? Что вернёт `getClass()`?
- Когда `@ManyToOne(optional = false)` помогает Hibernate сгенерировать более эффективный SQL?

## Материалы

- Vlad Mihalcea — *EAGER fetching is a code smell*
- Thorben Janssen — *Entity Mappings: Introduction to JPA FetchTypes*
