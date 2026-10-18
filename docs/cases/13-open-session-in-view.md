# Кейс 13. Open Session In View

**Эндпоинт:** `GET /api/products/{id}`
**Код:** `ProductService.getProduct`, `ProductController.details`, `application.yml` (`spring.jpa.open-in-view`)
**Тест:** `Case13OpenSessionInViewTest`

## Ситуация

Сервис возвращает в контроллер **сущность**, а контроллер собирает DTO, обращаясь к lazy-связям
(категория, теги, отзывы, авторы отзывов). Метод сервиса даже не транзакционный. И всё работает. Почему?

Попробуйте выключить `spring.jpa.open-in-view: false` и вызвать эндпоинт ещё раз.

```bash
curl "http://localhost:8080/api/products/1"
```

Интересный факт: если удалить это свойство из `application.yml`, Spring Boot при старте выведет warning
`spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering...`.
Почему разработчики Spring Boot не выключили OSIV по умолчанию, а ограничились предупреждением?

## Задание

1. Объясните, что делает `OpenEntityManagerInViewInterceptor` и на сколько «живёт» соединение/сессия при OSIV.
2. Выключите OSIV и исправьте код так, чтобы эндпоинт работал. Главный вопрос — **где** должна быть граница
   транзакции и **кто** отвечает за то, какие данные загрузить.
3. Найдите и исправьте другие места, которые ломаются без OSIV (прогоните smoke-тесты с `open-in-view: false`).
4. Подумайте: почему OSIV ещё и маскирует N+1?

**Готово, когда:** `Case13OpenSessionInViewTest` зелёный (он запускает приложение с `open-in-view=false`).
Рекомендуется также выключить OSIV в `application.yml` и убедиться, что всё остальное работает.

## Подсказки

<details><summary>Подсказка 1</summary>

Сервисный слой должен возвращать данные, готовые к использованию вне транзакции: DTO, а не managed-сущности.
</details>

<details><summary>Подсказка 2</summary>

`@Transactional(readOnly = true)` на чтении — что оно даёт кроме транзакции?
(Hibernate не делает dirty checking и flush, Spring может направить запрос на реплику.)
</details>

## Вопросы для самопроверки

- Почему OSIV называют антипаттерном? Какие у него аргументы «за»?
- Что такое `LazyInitializationException` и почему «включить `hibernate.enable_lazy_load_no_trans`» — плохое решение?
- Чем опасна сериализация сущностей напрямую в JSON (Jackson)? (lazy-прокси, бесконечная рекурсия, утечка полей)
- Как работает `@Transactional` в Spring (прокси, self-invocation, rollback rules для checked exceptions)?

## Материалы

- Vlad Mihalcea — *The Open Session In View Anti-Pattern*
- Spring Boot issue #7107 — обсуждение, почему OSIV включён по умолчанию
