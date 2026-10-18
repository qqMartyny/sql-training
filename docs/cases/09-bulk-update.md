# Кейс 09. Массовый update

**Эндпоинт:** `POST /api/products/reprice?categoryId=51&percent=5`
**Код:** `ProductService.reprice`
**Тест:** `Case09BulkUpdateTest`

## Ситуация

Маркетинг поднимает цены всех товаров категории на N процентов. Код загружает товары, меняет цену в цикле,
а Hibernate через dirty checking отправляет по `update` на каждый товар.

## Как воспроизвести

```bash
curl -X POST "http://localhost:8080/api/products/reprice?categoryId=51&percent=5"
```

## Задание

1. Сделайте изменение цен одним SQL-запросом. Округление должно совпадать (до копеек, `HALF_UP`).
2. Разберитесь, что происходит с persistence context после bulk update. Напишите (для себя) тест:
   загрузить товар → выполнить bulk update в той же транзакции → прочитать цену из сущности. Что вы увидите?
3. Сравните bulk update с подходом «batch update через dirty checking» (`batch_size` + `order_updates`):
   когда какой уместен? Что с `@Version`, entity listeners, кешем второго уровня, аудитом (Envers)?

**Готово, когда:** `Case09BulkUpdateTest` зелёный.

## Подсказки

<details><summary>Подсказка 1</summary>

`@Modifying` + `@Query("update ...")`. Посмотрите атрибуты `clearAutomatically` и `flushAutomatically`.
</details>

<details><summary>Подсказка 2</summary>

Метод с `@Modifying` может вернуть `int` — количество изменённых строк.
</details>

## Вопросы для самопроверки

- Почему bulk update «обходит» persistence context, и чем это опасно?
- Что такое dirty checking и как Hibernate его реализует (snapshot, bytecode enhancement)?
- Как обновить большую таблицу (100 млн строк) и не положить прод? (пачками, `vacuum`, блокировки, репликация)

## Материалы

- Thorben Janssen — *Spring Data JPA: How to use @Modifying*
- Vlad Mihalcea — *Bulk Update and Delete with JPA and Hibernate*
