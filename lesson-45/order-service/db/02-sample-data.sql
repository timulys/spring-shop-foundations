INSERT INTO products (code, name, category, unit_price_won)
VALUES
    ('P001', '키보드', '주변기기', 30000),
    ('P002', '마우스', '주변기기', 15000),
    ('P003', '노트', '문구', 5000)
ON CONFLICT (code) DO NOTHING;
