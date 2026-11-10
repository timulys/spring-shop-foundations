-- psql에서 필요한 문장만 골라 실행해 주세요.
SELECT code, name, category, unit_price_won
FROM products
ORDER BY code;

SELECT code, name, unit_price_won
FROM products
WHERE category = '주변기기' AND unit_price_won <= 20000
ORDER BY code;

-- 대상 행을 먼저 조회한 다음 같은 WHERE 조건으로 변경합니다.
SELECT code, name, unit_price_won FROM products WHERE code = 'P001';
UPDATE products SET unit_price_won = 32000 WHERE code = 'P001';
SELECT code, name, unit_price_won FROM products WHERE code = 'P001';

-- 확인 후 기본 샘플 가격으로 되돌립니다.
UPDATE products SET unit_price_won = 30000 WHERE code = 'P001';

-- 아래 문장들은 각각 오류가 나야 합니다. 하나씩 실행해 비교해 주세요.
INSERT INTO products VALUES ('P001', '중복 상품', '문구', 1000);
INSERT INTO products VALUES ('P004', '무료 상품', '문구', 0);
INSERT INTO products VALUES ('P004', NULL, '문구', 1000);

-- 실패 후에도 원래 세 상품과 가격이 유지되어야 합니다.
SELECT code, name, category, unit_price_won FROM products ORDER BY code;
