# Кейс 05. Фильтрация в памяти, динамические запросы, поиск по подстроке

**Эндпоинт:** `GET /api/products/search?q=&minPrice=&maxPrice=&categoryId=&inStock=`
**Код:** `ProductService.search`
**Тест:** `Case05FilteringInMemoryTest`

## Ситуация

Поиск товаров с необязательными фильтрами. Чтобы не писать 2⁵ = 32 метода репозитория под все комбинации
фильтров, разработчик сделал `findAll()` и отфильтровал стримом. На 5 000 товаров «работает».
На 5 000 000 — `OutOfMemoryError`.

## Как воспроизвести

```bash
curl "http://localhost:8080/api/products/search?q=phone%20pro&minPrice=1000"
curl "http://localhost:8080/api/products/search?categoryId=60&inStock=true"
```

## Задание

**Часть 1. Динамический запрос.**
Перенесите фильтрацию в БД. Каждый фильтр необязательный. Рассмотрите варианты и выберите один, объяснив выбор:
- JPA Criteria API / Spring Data `Specification`;
- JPQL с конструкцией `(:param is null or ...)` — и почему с ней бывают проблемы у планировщика;
- Query by Example;
- (для сравнения) jOOQ / Querydsl — только почитать.

**Часть 2. Поиск по подстроке.**
`name ilike '%phone pro%'` не может использовать обычный B-tree индекс. Разберитесь почему и сделайте так,
чтобы поиск по подстроке использовал индекс.

**Готово, когда:** `Case05FilteringInMemoryTest` зелёный.

## Подсказки

<details><summary>Подсказка 1</summary>

`JpaSpecificationExecutor<T>` + `Specification.where(...).and(...)`. Каждый фильтр — отдельный метод, который
возвращает `null` (или «пустую» спецификацию), если параметр не задан.
</details>

<details><summary>Подсказка 2</summary>

B-tree подходит для `like 'abc%'` (префикс) — и то при правильном collation/operator class (`text_pattern_ops`).
Для произвольной подстроки посмотрите расширение `pg_trgm` и индексы GIN/GiST. Расширение тоже создаётся миграцией.
</details>

<details><summary>Подсказка 3</summary>

Возвращайте из запроса сразу `ProductDto` (проекция), а не сущность — тогда и категория не понадобится.
</details>

## Вопросы для самопроверки

- Почему `lower(name) like lower(:q)` не использует индекс по `name`?
- Что такое селективность и почему индекс по `stock > 0` скорее всего бесполезен?
- Как защититься от SQL-инъекции при динамических запросах? Почему Criteria API и bind-параметры безопасны?
- Чем полнотекстовый поиск (`tsvector`) отличается от триграмм? Когда что выбрать?

## Материалы

- Spring Data JPA Reference — *Specifications*
- PostgreSQL docs — *pg_trgm*, *Index Types*
- Markus Winand — *Use The Index, Luke!* (раздел про LIKE)
