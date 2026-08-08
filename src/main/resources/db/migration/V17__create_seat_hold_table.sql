-- seat_hold table per DATABASE.md §6.1.
-- Temporarily holds a physical seat for a specific EventSession.
--
-- IMPORTANT — Concurrency strategy:
--   A PARTIAL unique index (WHERE status = 'ACTIVE') is used instead of a full
--   UNIQUE constraint. This allows EXPIRED and CANCELLED rows to remain in the
--   table as audit history without blocking new holds for the same seat+session.
--   A full UNIQUE(event_session_id, seat_id) would make expired rows permanently
--   block re-holding, violating the rule "expired hold must NOT block new holds."
--
-- Normal indexes per DATABASE.md §11 Index Strategy.
CREATE TABLE seat_hold (
    id               BIGSERIAL    PRIMARY KEY,
    hold_token       VARCHAR(36)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id          BIGINT       NOT NULL REFERENCES users(id),
    event_session_id BIGINT       NOT NULL REFERENCES event_session(id),
    seat_id          BIGINT       NOT NULL REFERENCES seat(id)
);

-- Partial unique index: enforces uniqueness only for ACTIVE holds.
-- EXPIRED and CANCELLED rows are excluded → they do not block new holds.
-- This is the primary database-level protection against concurrent double-holds.
CREATE UNIQUE INDEX uq_seat_hold_active
    ON seat_hold (event_session_id, seat_id)
    WHERE status = 'ACTIVE';

-- Normal indexes for query performance.
CREATE INDEX idx_seat_hold_user_id       ON seat_hold (user_id);
CREATE INDEX idx_seat_hold_event_session ON seat_hold (event_session_id);
CREATE INDEX idx_seat_hold_expired_at    ON seat_hold (expired_at);
