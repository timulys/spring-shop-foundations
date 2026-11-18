ALTER TABLE carts ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE carts ADD CONSTRAINT carts_revision_nonnegative CHECK (revision >= 0);
