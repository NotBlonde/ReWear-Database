-- =============================================
-- ReWear Webshop Database (MySQL 8+)
-- Vintage/Secondhand + Upcycled clothing
-- =============================================

-- (valfritt) sätt teckenkodning och säker SQL-mode för demo
SET NAMES utf8mb4;
SET SESSION sql_mode='STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

DROP DATABASE IF EXISTS rewear_db;
CREATE DATABASE rewear_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE rewear_db;

-- ---------- Lookups ----------
CREATE TABLE brands (
  brand_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB COMMENT='Clothing brands (includes ReWear as in-house brand).';

CREATE TABLE categories (
  category_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB COMMENT='Product categories';

-- ---------- Core product model ----------
CREATE TABLE products (
  product_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  brand_id INT NOT NULL,
  CONSTRAINT fk_products_brand FOREIGN KEY (brand_id)
    REFERENCES brands(brand_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Product model (e.g., Plain T-Shirt, Vintage Jeans).';

CREATE INDEX ix_products_brand ON products(brand_id);

CREATE TABLE product_variants (
  variant_id INT AUTO_INCREMENT PRIMARY KEY,
  product_id INT NOT NULL,
  size VARCHAR(20) NOT NULL,
  color VARCHAR(40) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  sku VARCHAR(64) NOT NULL UNIQUE,
  stock_qty INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_variants_product FOREIGN KEY (product_id)
    REFERENCES products(product_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_variant_qty CHECK (stock_qty >= 0)
) ENGINE=InnoDB COMMENT='Sellable variants (size + color) with price and stock.';

CREATE INDEX ix_variants_attr ON product_variants(product_id, color, size);

-- M:N products <-> categories
CREATE TABLE product_categories (
  product_id INT NOT NULL,
  category_id INT NOT NULL,
  PRIMARY KEY (product_id, category_id),
  CONSTRAINT fk_pc_product FOREIGN KEY (product_id)
    REFERENCES products(product_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_pc_category FOREIGN KEY (category_id)
    REFERENCES categories(category_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------- Customers & orders ----------
CREATE TABLE customers (
  customer_id INT AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(80) NOT NULL,
  last_name  VARCHAR(80) NOT NULL,
  city       VARCHAR(120) NOT NULL,
  email      VARCHAR(150) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE INDEX ix_customers_city ON customers(city);

CREATE TABLE orders (
  order_id INT AUTO_INCREMENT PRIMARY KEY,
  customer_id INT NOT NULL,
  order_date DATE NOT NULL,
  status ENUM('NEW','PAID','SHIPPED','CANCELLED') NOT NULL DEFAULT 'NEW',
  CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
    REFERENCES customers(customer_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX ix_orders_date ON orders(order_date);

CREATE TABLE order_items (
  order_item_id INT AUTO_INCREMENT PRIMARY KEY,
  order_id  INT NOT NULL,
  variant_id INT NOT NULL,
  quantity  INT NOT NULL CHECK (quantity > 0),
  unit_price DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_items_order FOREIGN KEY (order_id)
    REFERENCES orders(order_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_items_variant FOREIGN KEY (variant_id)
    REFERENCES product_variants(variant_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Stores unit_price snapshot at purchase time.';

CREATE INDEX ix_items_variant ON order_items(variant_id);

-- ---------- Seed data ----------
INSERT INTO brands(name) VALUES
  ('ReWear'), ('VintageCo'), ('UrbanRevive'), ('NordicSecond'), ('UpcycleLab');

INSERT INTO categories(name) VALUES
  ('Tops'), ('Pants'), ('Jackets'), ('Dresses'), ('Accessories'),
  ('Sweaters'), ('Casual'), ('Sportswear');

-- Products (generic English names)
INSERT INTO products(name, brand_id) VALUES
  ('Plain T-Shirt', 1),            -- ReWear
  ('Upcycled Hoodie', 1),          -- ReWear
  ('Vintage Jeans', 2),            -- VintageCo
  ('Denim Jacket', 2),             -- VintageCo
  ('Summer Dress', 3),             -- UrbanRevive
  ('Patchwork Sweater', 5),        -- UpcycleLab
  ('Canvas Tote Bag', 1),          -- ReWear
  ('Chino Pants', 1);              -- ReWear

-- Product -> categories
INSERT INTO product_categories
SELECT p.product_id, c.category_id FROM products p, categories c
WHERE p.name='Plain T-Shirt' AND c.name IN ('Tops','Casual');

INSERT INTO product_categories
SELECT p.product_id, c.category_id FROM products p, categories c
WHERE p.name='Upcycled Hoodie' AND c.name IN ('Tops','Sweaters','Casual');

INSERT INTO product_categories
SELECT p.product_id, c.category_id FROM products p, categories c
WHERE p.name='Vintage Jeans' AND c.name IN ('Pants','Casual');

INSERT INTO product_categories
SELECT p.product_id, c.category_id FROM products p, categories c
WHERE p.name='Denim Jacket' AND c.name IN ('Jackets','Casual');

INSERT INTO product_categories
SELECT p.product_id, c.category_id FROM products p, categories c
WHERE p.name='Summer Dress' AND c.name IN ('Dresses','Casual');

INSERT INTO product_categories
SELECT p.product_id, c.category_id FROM products p, categories c
WHERE p.name='Patchwork Sweater' AND c.name IN ('Sweaters','Casual');

INSERT INTO product_categories
SELECT p.product_id, c.category_id FROM products p, categories c
WHERE p.name='Canvas Tote Bag' AND c.name IN ('Accessories','Casual');

INSERT INTO product_categories
SELECT p.product_id, c.category_id FROM products p, categories c
WHERE p.name='Chino Pants' AND c.name IN ('Pants','Casual');

-- Variants
INSERT INTO product_variants(product_id, size, color, price, sku, stock_qty) VALUES
  ((SELECT product_id FROM products WHERE name='Plain T-Shirt'),'S','Pink',149.00,'RW-TS-S-PNK',30),
  ((SELECT product_id FROM products WHERE name='Plain T-Shirt'),'M','Blue',149.00,'RW-TS-M-BLU',40),
  ((SELECT product_id FROM products WHERE name='Upcycled Hoodie'),'M','Navy',399.00,'RW-HD-M-NVY',20),
  ((SELECT product_id FROM products WHERE name='Vintage Jeans'),'38','Blue',349.00,'VC-JE-38-BLU',18),
  ((SELECT product_id FROM products WHERE name='Denim Jacket'),'M','Blue',499.00,'VC-DJ-M-BLU',12),
  ((SELECT product_id FROM products WHERE name='Summer Dress'),'38','Red',299.00,'UR-DR-38-RED',16),
  ((SELECT product_id FROM products WHERE name='Patchwork Sweater'),'M','Blue',379.00,'UL-SW-M-BLU',14),
  ((SELECT product_id FROM products WHERE name='Canvas Tote Bag'),'OneSize','Blue',99.00,'RW-TB-OS-BLU',80),
  ((SELECT product_id FROM products WHERE name='Chino Pants'),'38','Black',329.00,'RW-CH-38-BLK',22),
  ((SELECT product_id FROM products WHERE name='Chino Pants'),'36','Khaki',329.00,'RW-CH-36-KHK',10);

-- Customers
INSERT INTO customers(first_name,last_name,city,email) VALUES
  ('Anna','Svensson','Stockholm','anna.s@example.com'),
  ('Björn','Karlsson','Göteborg','bjorn.k@example.com'),
  ('Carla','Nilsson','Malmö','carla.n@example.com'),
  ('David','Öberg','Uppsala','david.o@example.com'),
  ('Elin','Hansson','Lund','elin.h@example.com');

-- Orders (spread across months)
INSERT INTO orders(customer_id, order_date, status) VALUES
  ((SELECT customer_id FROM customers WHERE email='anna.s@example.com'),'2025-06-15','PAID'),
  ((SELECT customer_id FROM customers WHERE email='bjorn.k@example.com'),'2025-06-20','PAID'),
  ((SELECT customer_id FROM customers WHERE email='carla.n@example.com'),'2025-07-05','PAID'),
  ((SELECT customer_id FROM customers WHERE email='david.o@example.com'),'2025-07-18','PAID'),
  ((SELECT customer_id FROM customers WHERE email='elin.h@example.com'),'2025-08-02','PAID'),
  ((SELECT customer_id FROM customers WHERE email='anna.s@example.com'),'2025-08-21','PAID');

-- Order items
INSERT INTO order_items(order_id, variant_id, quantity, unit_price) VALUES
  -- 1) Anna: Chino Pants 38 Black + Tote Bag x2
  (1, (SELECT variant_id FROM product_variants pv
       JOIN products p ON p.product_id=pv.product_id
       JOIN brands   b ON b.brand_id=p.brand_id
       WHERE p.name='Chino Pants' AND pv.size='38' AND pv.color='Black' AND b.name='ReWear' LIMIT 1), 1, 329.00),
  (1, (SELECT variant_id FROM product_variants WHERE sku='RW-TB-OS-BLU'), 2, 99.00),

  -- 2) Björn: Vintage Jeans 38 Blue x2
  (2, (SELECT variant_id FROM product_variants WHERE sku='VC-JE-38-BLU'), 2, 349.00),

  -- 3) Carla: Summer Dress 38 Red + Plain T-Shirt M Blue
  (3, (SELECT variant_id FROM product_variants WHERE sku='UR-DR-38-RED'), 1, 299.00),
  (3, (SELECT variant_id FROM product_variants WHERE sku='RW-TS-M-BLU'), 1, 149.00),

  -- 4) David: Denim Jacket M Blue + Upcycled Hoodie M Navy
  (4, (SELECT variant_id FROM product_variants WHERE sku='VC-DJ-M-BLU'), 1, 499.00),
  (4, (SELECT variant_id FROM product_variants WHERE sku='RW-HD-M-NVY'), 1, 399.00),

  -- 5) Elin: Chino Pants 36 Khaki + Tote Bag x3
  (5, (SELECT variant_id FROM product_variants WHERE sku='RW-CH-36-KHK'), 1, 329.00),
  (5, (SELECT variant_id FROM product_variants WHERE sku='RW-TB-OS-BLU'), 3, 99.00),

  -- 6) Anna: Plain T-Shirt S Pink + Patchwork Sweater M Blue
  (6, (SELECT variant_id FROM product_variants WHERE sku='RW-TS-S-PNK'), 1, 149.00),
  (6, (SELECT variant_id FROM product_variants WHERE sku='UL-SW-M-BLU'), 1, 379.00);
