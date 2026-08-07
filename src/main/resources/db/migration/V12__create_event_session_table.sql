-- event_session table per DATABASE.md §5.5.
-- Unique constraint: (event_id, venue_id, start_time) prevents duplicate sessions
-- for the same event at the same venue and start time.
-- base_price is DECIMAL to represent currency correctly.
-- status stored as VARCHAR per project enum convention.
CREATE TABLE event_session (
    id            BIGSERIAL       PRIMARY KEY,
    start_time    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    booking_open  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    booking_close TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    base_price    DECIMAL(15, 2)  NOT NULL,
    status        VARCHAR(50)     NOT NULL,
    event_id      BIGINT          NOT NULL REFERENCES event(id),
    venue_id      BIGINT          NOT NULL REFERENCES venue(id),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (event_id, venue_id, start_time)
);
