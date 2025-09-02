USE rewear_db;

-- 1) Customers who bought black pants in size 38 of brand ReWear
SELECT DISTINCT c.first_name, c.last_name
FROM customers c
JOIN orders o       ON o.customer_id = c.customer_id
JOIN order_items oi ON oi.order_id = o.order_id
JOIN product_variants pv ON pv.variant_id = oi.variant_id
JOIN products p     ON p.product_id = pv.product_id
JOIN brands b       ON b.brand_id = p.brand_id
JOIN product_categories pc ON pc.product_id = p.product_id
JOIN categories cat ON cat.category_id = pc.category_id
WHERE b.name = 'ReWear'
  AND pv.color = 'Black'
  AND pv.size  = '38'
  AND cat.name = 'Pants';

-- 2) Count of products per category (distinct models)
SELECT cat.name AS category, COUNT(DISTINCT pc.product_id) AS product_count
FROM categories cat
LEFT JOIN product_categories pc ON pc.category_id = cat.category_id
GROUP BY cat.category_id, cat.name
ORDER BY product_count DESC, category;

-- 3) Total spend per customer
SELECT c.first_name, c.last_name,
       SUM(oi.quantity * oi.unit_price) AS total_spent
FROM customers c
JOIN orders o ON o.customer_id = c.customer_id
JOIN order_items oi ON oi.order_id = o.order_id
GROUP BY c.customer_id, c.first_name, c.last_name
ORDER BY total_spent DESC;

-- 4) Total order value per city where value > 1000 SEK
SELECT c.city,
       SUM(oi.quantity * oi.unit_price) AS city_total
FROM customers c
JOIN orders o ON o.customer_id = c.customer_id
JOIN order_items oi ON oi.order_id = o.order_id
GROUP BY c.city
HAVING SUM(oi.quantity * oi.unit_price) > 1000
ORDER BY city_total DESC;

-- 5) Top-5 most sold products (by quantity across variants)
SELECT p.name AS product,
       SUM(oi.quantity) AS total_quantity
FROM order_items oi
JOIN product_variants pv ON pv.variant_id = oi.variant_id
JOIN products p ON p.product_id = pv.product_id
GROUP BY p.product_id, p.name
ORDER BY total_quantity DESC, product
LIMIT 5;

-- 6) Best-selling month (YYYY-MM) — utan CTE
SELECT m.year_month, m.total_sales
FROM (
  SELECT DATE_FORMAT(o.order_date, '%Y-%m') AS year_month,
         SUM(oi.quantity * oi.unit_price)   AS total_sales
  FROM orders o
  JOIN order_items oi ON oi.order_id = o.order_id
  GROUP BY DATE_FORMAT(o.order_date, '%Y-%m')
) m
WHERE m.total_sales = (
  SELECT MAX(total_sales) FROM (
    SELECT SUM(oi.quantity * oi.unit_price) AS total_sales
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    GROUP BY DATE_FORMAT(o.order_date, '%Y-%m')
  ) x
);
