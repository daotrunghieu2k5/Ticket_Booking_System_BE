-- Minimal booking table stub per DATABASE.md §6.2.
-- Required as a dependency for the booking_item table (V15).
-- Full booking columns and business logic will be implemented in the Booking module.
-- booking_code is UNIQUE per DATABASE.md business rules.
CREATE TABLE booking (
    id               BIGSERIAL       PRIMARY KEY,
    booking_code     VARCHAR(50)     NOT NULL UNIQUE,
    total_amount     DECIMAL(15, 2)  NOT NULL,
    status           VARCHAR(50)     NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id          BIGINT          NOT NULL REFERENCES "users"(id),
    event_session_id BIGINT          NOT NULL REFERENCES event_session(id)
);
