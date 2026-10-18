# Кейс 07. Глубокая offset-пагинация → keyset

**Эндпоинты:** `GET /api/orders/feed?page=&size=` (есть), `GET /api/orders/feed/keyset?afterCreatedAt=&afterId=&size=` (нужно реализовать)
**Код:** `OrderService.feed`, `OrderService.feedAfter`
**Тест:** `Case07KeysetPaginationTest`

## Ситуация

Лента заказов для бэк-офиса и выгрузки в интеграции. Используется стандартный `Pageable`.
Внешняя система выгружает все заказы, листая страницы по 20 штук: `page=0, 1, 2, ..., 15000`.

## Как воспроизвести

**Сначала пройдите кейс 06** (без индексов разница не видна — всё одинаково медленно).

```bash
curl "http://localhost:8080/api/orders/feed?page=0&size=20"
curl "http://localhost:8080/api/orders/feed?page=10000&size=20"
```

Выполните оба SQL из лога через `explain (analyze, buffers)`. Сравните `rows` и `Buffers` на `Index Scan`.
Сколько запросов выполняет каждый вызов? Что делает второй запрос?

## Задание

1. Объясните, почему `offset 200000` медленный даже при наличии индекса.
2. Реализуйте `OrderService.feedAfter` — keyset (seek) пагинацию: следующая страница начинается после
   последнего элемента предыдущей. Сортировка такая же: `created_at desc, id desc`.
3. Убедитесь, что запрос — один (без count и без N+1), и его скорость не зависит от «глубины».
4. Подумайте о проблеме: что будет с offset-пагинацией, если между запросами страниц добавились новые заказы?
   А с keyset?

**Готово, когда:** `Case07KeysetPaginationTest` зелёный.

## Подсказки

<details><summary>Подсказка 1</summary>

Зачем в сортировке `id`? `created_at` не уникален. Без уникального «хвоста» в сортировке пагинация недетерминирована.
</details>

<details><summary>Подсказка 2</summary>

Условие «после (c, id)» при сортировке по убыванию: `created_at < :c or (created_at = :c and id < :id)`.
В PostgreSQL то же самое можно записать как сравнение кортежей: `(created_at, id) < (:c, :id)` — сравните планы обоих вариантов.
Работает ли сравнение кортежей в JPQL? А в native query?
</details>

<details><summary>Подсказка 3</summary>

В Spring Data есть встроенная поддержка: `ScrollPosition`, `KeysetScrollPosition`, `Window<T>`.
Попробуйте и её, и ручной вариант. Какой SQL генерирует Spring Data?
</details>

## Вопросы для самопроверки

- Какие ограничения у keyset-пагинации? (нельзя перейти на страницу 500, сложнее с произвольной сортировкой)
- Как в REST API отдают курсор клиенту (opaque cursor, base64)?
- Какой индекс нужен под keyset-запрос и почему именно такой?

## Материалы

- Markus Winand — *We need tool support for keyset pagination* / *Paging Through Results* (use-the-index-luke.com)
- Spring Data Commons Reference — *Scrolling*
