ALTER TABLE products
  ADD COLUMN is_new TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN is_hot TINYINT(1) NOT NULL DEFAULT 0;

CREATE INDEX idx_products_is_new ON products(is_new);
CREATE INDEX idx_products_is_hot ON products(is_hot);
