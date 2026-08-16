-- US-14: Create payment_transaction table.
--
-- Each row represents one payment attempt (one call to PayOS gateway).
-- A single Payment may have multiple PaymentTransactions (due to retries).
-- transaction_code is UNIQUE — enforces idempotency at the DB level.
-- gateway_payload stores raw gateway response for audit purposes.

CREATE TABLE payment_transaction
(
    id               BIGSERIAL    PRIMARY KEY,
    provider         VARCHAR(50)  NOT NULL,
    transaction_code VARCHAR(255) NOT NULL UNIQUE,
    request_id       VARCHAR(255),
    amount           DECIMAL(15, 2) NOT NULL,
    currency         VARCHAR(10),
    response_code    VARCHAR(50),
    response_message VARCHAR(500),
    gateway_payload  TEXT,
    status           VARCHAR(50)  NOT NULL,
    transaction_time TIMESTAMP WITHOUT TIME ZONE,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_id       BIGINT       NOT NULL REFERENCES payment (id)
);

CREATE INDEX idx_payment_transaction_payment_id
    ON payment_transaction (payment_id);
