-- Minimal booking_item table stub per DATABASE.md §6.3.
-- Required so BookingItemRepository.existsBySeatId() can be used for the
-- delete-safety check in SeatServiceImpl (BR-07).
-- qr_code is UNIQUE per DATABASE.md business rules.
-- event_snapshot stores an immutable snapshot of ticket info (TEXT/JSON).
-- Unique constraint (event_session_id, seat_id): one seat sold once per session.
CREATE TABLE booking_item (
    id               BIGSERIAL       PRIMARY KEY,
    qr_code          VARCHAR(255)    NOT NULL UNIQUE,
    price            DECIMAL(15, 2)  NOT NULL,
    status           VARCHAR(50)     NOT NULL,
    event_snapshot   TEXT,
    booking_id       BIGINT          NOT NULL REFERENCES booking(id),
    seat_id          BIGINT          NOT NULL REFERENCES seat(id),
    event_session_id BIGINT          NOT NULL REFERENCES event_session(id),
    UNIQUE (event_session_id, seat_id)
);
