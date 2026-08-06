CREATE TABLE IF NOT EXISTS customer (
    customer_id       VARCHAR(255) PRIMARY KEY,
    name              VARCHAR(255),
    membership_level  VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    purchase_count    INTEGER NOT NULL DEFAULT 0
);

ALTER TABLE flash_sale
    ADD COLUMN early_access_start TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE flash_sale
    ALTER COLUMN early_access_start DROP DEFAULT;
