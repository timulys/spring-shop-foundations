SELECT version, description, checksum, success
FROM flyway_schema_history ORDER BY installed_rank;

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema='public' AND table_name='orders'
ORDER BY ordinal_position;

SELECT id, status, total_won, cancelled_at FROM orders ORDER BY id;
