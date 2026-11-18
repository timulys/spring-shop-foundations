DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM cart_items;
DELETE FROM carts;
DELETE FROM members;
INSERT INTO members(id, login_id, display_name, password_hash) VALUES
(100001, 'fixture_a', '회원 A', '$2a$04$.....................................................'),
(100002, 'fixture_b', '회원 B', '$2a$04$.....................................................');
INSERT INTO carts(id,member_id,revision) VALUES (1,100001,0);
SELECT setval(pg_get_serial_sequence('carts','id'), 2, false);
