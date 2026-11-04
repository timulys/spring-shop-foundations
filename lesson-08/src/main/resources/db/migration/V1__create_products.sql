CREATE TABLE products (
    code varchar(20) PRIMARY KEY,
    name varchar(100) NOT NULL,
    category varchar(40) NOT NULL,
    unit_price_won bigint NOT NULL,
    CONSTRAINT products_price_range
        CHECK (unit_price_won BETWEEN 1 AND 1000000)
);

INSERT INTO products (code, name, category, unit_price_won)
VALUES
    ('P001', '키보드', '주변기기', 30000),
    ('P002', '마우스', '주변기기', 15000),
    ('P003', '노트', '문구', 5000);
