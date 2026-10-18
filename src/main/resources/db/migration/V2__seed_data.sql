-- Генерация тестовых данных.
-- Объём регулируется плейсхолдером Flyway ${seed_scale} (spring.flyway.placeholders.seed_scale):
--   1.0 -> ~20k клиентов, 5k товаров, 300k заказов, ~900k позиций, 60k отзывов
-- Первая миграция на scale=1 занимает порядка минуты.
-- Скрипт не рассчитывает на то, что id начинаются с 1: sequence не откатываются вместе с транзакцией,
-- поэтому после неудачной попытки миграции id будут начинаться с другого значения.

-- Приложение ограничивает statement_timeout (см. application.yml), а генерация данных — долгая операция.
-- SET LOCAL действует только до конца транзакции миграции.
set local statement_timeout = 0;

-- Категории: 10 корневых -> по 4 подкатегории -> по 3 листовых (170 штук)
insert into categories (name, parent_id)
select 'Category ' || g, null
from generate_series(1, 10) g;

insert into categories (name, parent_id)
select p.name || '.' || g, p.id
from categories p
         cross join generate_series(1, 4) g
where p.parent_id is null;

insert into categories (name, parent_id)
select c.name || '.' || g, c.id
from categories c
         cross join generate_series(1, 3) g
where c.parent_id is not null;

-- Клиенты. Email хранится в смешанном регистре — это пригодится в кейсе про индексы.
insert into customers (email, first_name, last_name, city, registered_at)
select 'User' || g || '@Mail.example',
       (array ['Ivan','Anna','Petr','Olga','Sergey','Maria','Dmitry','Elena','Alexey','Natalia'])[1 + g % 10],
       (array ['Ivanov','Smirnov','Kuznetsov','Popov','Vasiliev','Petrov','Sokolov','Mikhailov','Novikov','Fedorov'])[1 + (g / 10) % 10],
       (array ['Moscow','Saint Petersburg','Kazan','Novosibirsk','Yekaterinburg','Nizhny Novgorod','Samara','Omsk','Rostov','Ufa'])[1 + (g * 7) % 10],
       now() - (random() * interval '1500 days')
from generate_series(1, greatest(1, (20000 * ${seed_scale})::int)) g;

-- Товары распределены только по листовым категориям
insert into products (sku, name, description, price, stock, category_id, created_at)
select 'SKU-' || lpad(g::text, 8, '0'),
       (array ['Phone','Laptop','Headphones','Keyboard','Mouse','Monitor','Tablet','Camera','Speaker','Watch'])[1 + g % 10]
           || ' ' || (array ['Pro','Max','Lite','Ultra','Mini','Plus','Air','Neo'])[1 + (g / 10) % 8]
           || ' ' || g,
       'Description of product ' || g || '. ' || repeat('Lorem ipsum dolor sit amet. ', 5),
       round((100 + random() * 99900)::numeric, 2),
       (random() * 100)::int,
       leaves.ids[1 + (g % array_length(leaves.ids, 1))],
       now() - (random() * interval '1000 days')
from generate_series(1, greatest(1, (5000 * ${seed_scale})::int)) g
         cross join (select array_agg(c.id order by c.id) as ids
                     from categories c
                     where not exists (select 1 from categories ch where ch.parent_id = c.id)) leaves;

insert into tags (name)
select 'tag-' || g
from generate_series(1, 30) g;

insert into product_tags (product_id, tag_id)
select distinct p.id, tg.min_id + ((p.id * 31 + t) * 17) % 30
from products p
         cross join generate_series(1, 3) t
         cross join (select min(id) as min_id from tags) tg
where (p.id + t) % 4 <> 0;

-- 20% клиентов (с самыми большими id) не делали заказов вовсе
insert into orders (customer_id, status, created_at)
select c.min_id + floor(random() * greatest(1, (20000 * ${seed_scale} * 0.8)::int))::bigint,
       case
           when r < 0.05 then 'NEW'
           when r < 0.20 then 'PAID'
           when r < 0.40 then 'SHIPPED'
           when r < 0.95 then 'DELIVERED'
           else 'CANCELLED'
           end,
       now() - (random() * interval '730 days')
from (select random() as r
      from generate_series(1, greatest(1, (300000 * ${seed_scale})::int))) s
         cross join (select min(id) as min_id from customers) c;

-- От 1 до 5 позиций на заказ. Цена позиции фиксируется на момент покупки.
insert into order_items (order_id, product_id, quantity, price)
select x.order_id, p.id, x.quantity, p.price
from (select o.id                                                                       as order_id,
             pr.min_id + floor(random() * greatest(1, (5000 * ${seed_scale})::int))::bigint + 0 * o.id as product_id,
             1 + floor(random() * 3)::int                                                       as quantity
      from orders o
               cross join lateral generate_series(1, 1 + (o.id % 5)::int) n
               cross join (select min(id) as min_id from products) pr) x
         join products p on p.id = x.product_id;

insert into reviews (product_id, customer_id, rating, comment, created_at)
select pr.min_id + floor(random() * greatest(1, (5000 * ${seed_scale})::int))::bigint,
       c.min_id + floor(random() * greatest(1, (20000 * ${seed_scale})::int))::bigint,
       1 + floor(random() * 5)::int,
       'Review text ' || g,
       now() - (random() * interval '700 days')
from generate_series(1, greatest(1, (60000 * ${seed_scale})::int)) g
         cross join (select min(id) as min_id from products) pr
         cross join (select min(id) as min_id from customers) c;

analyze;
