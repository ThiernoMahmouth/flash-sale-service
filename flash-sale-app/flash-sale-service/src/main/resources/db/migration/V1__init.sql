CREATE TABLE IF NOT EXISTS product (
    id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS flash_sale (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL REFERENCES product (id),
    total_stock INTEGER NOT NULL,
    sold_stock  INTEGER NOT NULL DEFAULT 0,
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP NOT NULL,
    version     BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS purchase_request (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flash_sale_id UUID NOT NULL REFERENCES flash_sale (id),
    customer_id   VARCHAR(255) NOT NULL,
    quantity      INTEGER NOT NULL,
    requested_at  TIMESTAMP NOT NULL,
    decided_at    TIMESTAMP,
    status        VARCHAR(40) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_purchase_request_sale_status
    ON purchase_request (flash_sale_id, status);

CREATE TABLE IF NOT EXISTS purchase (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flash_sale_id       UUID NOT NULL REFERENCES flash_sale (id),
    purchase_request_id UUID NOT NULL REFERENCES purchase_request (id),
    customer_id         VARCHAR(255) NOT NULL,
    quantity            INTEGER NOT NULL,
    unit_price          NUMERIC(10, 2) NOT NULL,
    purchased_at        TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_event (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic        VARCHAR(255) NOT NULL,
    payload      TEXT NOT NULL,
    aggregate_id VARCHAR(255),
    status       VARCHAR(40) NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    published_at TIMESTAMP
);
