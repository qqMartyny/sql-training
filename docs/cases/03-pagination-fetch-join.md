# Кейс 03. Пагинация + join fetch коллекции, count-запрос

**Эндпоинт:** `GET /api/products/catalog?page=0&size=20`
**Код:** `ProductService.catalog`, `ProductRepository.findAllWithReviews`
**Тест:** `Case03PaginationWithFetchJoinTest`

## Ситуация

Каталог: страница из 20 товаров, у каждого — категория, количество отзывов и средний рейтинг.
Разработчик знал про N+1 и сделал `left join fetch p.reviews`. Стало быстрее, но всё ещё ~290 запросов на страницу.

## Как воспроизвести

```bash
curl "http://localhost:8080/api/products/catalog?page=0&size=20"
curl "http://localhost:8080/api/products/catalog?page=200&size=20"
```

Найдите в логах:
1. Основной запрос. Как Hibernate 7 реализовал пагинацию при `join fetch` коллекции?
2. Запрос для `totalElements` (count). Что он делает и почему это дорого?
3. Остальные ~280 запросов: откуда они?

## Задание

1. Объясните, как пагинация с `join fetch` коллекции работала в Hibernate 5/6 (`HHH90003004`,
   `hibernate.query.fail_on_pagination_over_collection_fetch`) и что изменилось в Hibernate 7.
2. Задайте вопрос: **а нужны ли здесь вообще сущности `Review`**? Что реально показывает каталог?
3. Перепишите выборку так, чтобы для страницы выполнялись ровно два запроса: данные страницы и count.
4. Сделайте count-запрос дешёвым.
5. Подумайте: нужен ли каталогу `totalElements` вообще? Что такое `Slice` и когда он лучше `Page`?

**Готово, когда:** `Case03PaginationWithFetchJoinTest` зелёный.

## Подсказки

<details><summary>Подсказка 1</summary>

Агрегаты (`count`, `avg`) умеет считать сама БД. Spring Data умеет возвращать из `@Query` не только сущности, но и
DTO (`select new ...`), интерфейсные проекции и record'ы.
</details>

<details><summary>Подсказка 2</summary>

У `@Query` есть атрибут `countQuery`. Посмотрите, как Spring Data выводит count-запрос автоматически и почему
для запросов с `join` это часто неоптимально.
</details>

<details><summary>Подсказка 3</summary>

Если вы всё же оставили загрузку сущностей — проверьте, не тянет ли каждый отзыв за собой клиента (кейс 02).
</details>

## Вопросы для самопроверки

- Почему `distinct` в JPQL с `join fetch` раньше был обязателен, а в Hibernate 6+ — нет?
- Что произойдёт с памятью приложения, если пагинация над коллекцией делается в памяти, а товаров миллион?
- Чем `Page` отличается от `Slice` с точки зрения SQL?
- Какие виды проекций есть в Spring Data JPA (интерфейсные closed/open, DTO-класс, record, dynamic)? Какие из них
  реально сужают SELECT, а какие — нет?

## Материалы

- Vlad Mihalcea — *The best way to fix the Hibernate "HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!" warning message*
- Spring Data JPA Reference — *Projections*
- Hibernate 7 Migration Guide
