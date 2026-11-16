CREATE TABLE IF NOT EXISTS products (
    code varchar(20) PRIMARY KEY,
    name varchar(100) NOT NULL,
    category varchar(40) NOT NULL,
    unit_price_won bigint NOT NULL,
    CONSTRAINT products_price_range
        CHECK (unit_price_won BETWEEN 1 AND 1000000)
);
