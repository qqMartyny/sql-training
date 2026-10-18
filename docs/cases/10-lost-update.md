# Кейс 10. Конкурентный доступ: lost update

**Эндпоинт:** `POST /api/orders`
**Код:** `OrderService.placeOrder`, `Product.stock`, `PaymentGateway`
**Тест:** `Case10LostUpdateTest`

## Ситуация

Распродажа: лимитированный товар, на складе 5 штук. Одновременно приходят 20 покупателей.
Продано 20 штук, а остаток на складе — 3 или 4. Склад в ярости.

```bash
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"customerId": 1, "items": [{"productId": 1, "quantity": 1}]}'
```

## Задание

1. Нарисуйте timeline двух параллельных транзакций и покажите, где теряется обновление.
   Какой уровень изоляции у PostgreSQL по умолчанию? Спасает ли `REPEATABLE READ`? А `SERIALIZABLE`?
2. Реализуйте **минимум два** решения и сравните их:
   - optimistic locking (`@Version`) + повтор операции (retry);
   - pessimistic locking (`select ... for update`);
   - атомарный условный `update ... set stock = stock - :q where id = :id and stock >= :q`.
3. Обратите внимание на вызов `paymentGateway.authorize()` внутри транзакции. Почему держать транзакцию
   (и соединение из пула!) во время сетевого вызова — плохо? Что будет при 100 одновременных заказах и пуле в 10 соединений?
   Как перестроить процесс?
4. Если у вас pessimistic lock и в заказе несколько товаров — возможен ли deadlock? Как его избежать?

**Готово, когда:** `Case10LostUpdateTest` зелёный: продано ровно 5, остаток 0, данные согласованы.

## Подсказки

<details><summary>Подсказка 1</summary>

Hibernate по умолчанию пишет в `update` абсолютное значение, вычисленное в Java: `set stock = 4`.
Две транзакции прочитали 5, обе записали 4.
</details>

<details><summary>Подсказка 2 — optimistic + retry</summary>

С `@Version` второй коммит упадёт с `ObjectOptimisticLockingFailureException`. Тест требует, чтобы
покупателям не отказывали, пока товар есть — значит, нужен retry. Retry должен повторять **всю транзакцию**,
а не один метод внутри неё. Почему? (посмотрите на прокси `@Transactional` и self-invocation).
Варианты: Spring Retry (`@Retryable`), ручной цикл вокруг `TransactionTemplate`.
</details>

<details><summary>Подсказка 3 — pessimistic</summary>

`@Lock(LockModeType.PESSIMISTIC_WRITE)` на методе репозитория. Таймаут блокировки — `jakarta.persistence.lock.timeout`.
</details>

## Вопросы для самопроверки

- Что такое lost update, dirty read, non-repeatable read, phantom read, write skew?
- Как MVCC в PostgreSQL реализует изоляцию? Почему читатели не блокируют писателей?
- Когда optimistic locking лучше pessimistic и наоборот?
- Нужна ли колонка `version`, если используется атомарный условный update?
- Что такое `@Transactional(readOnly = true)` и что оно реально меняет (Hibernate flush mode, драйвер, реплики)?

## Материалы

- Vlad Mihalcea — *A beginner's guide to database locking and the lost update phenomena*
- PostgreSQL docs — *Transaction Isolation*, *Explicit Locking*
- Martin Kleppmann — *Designing Data-Intensive Applications*, глава 7 «Transactions»
