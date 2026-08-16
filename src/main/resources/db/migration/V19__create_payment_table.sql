-- V19: Create payment table per DATABASE.md §6.4.
--
-- Relationships:
--   payment.booking_id → booking.id (1:1, enforced by UNIQUE)
--
-- Business rules:
--   One Booking has exactly one Payment.
--   Payment amount must equal Booking.total_amount.
--   Initial status when booking is created: PENDING.
--   payment_method is nullable at creation time — set when user selects payment gateway.
--   Payment history must never be physically deleted.
CREATE TABLE payment (
    id             BIGSERIAL       PRIMARY KEY,
    amount         DECIMAL(15, 2)  NOT NULL,
    payment_method VARCHAR(50),
    status         VARCHAR(50)     NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    booking_id     BIGINT          NOT NULL UNIQUE REFERENCES booking(id)
);

-- Index: payment lookups by status (e.g. find all PENDING payments).
CREATE INDEX idx_payment_status ON payment(status);
