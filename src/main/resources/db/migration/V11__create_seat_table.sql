-- seat table per DATABASE.md §5.3.
-- Seats are physical and permanent; they do NOT have audit columns (no created_at/updated_at).
-- Unique constraint: (venue_id, row_name, seat_number) prevents duplicate seat positions.
-- price_multiplier is DECIMAL to allow fractional values (e.g. 1.5 for VIP).
CREATE TABLE seat (
    id               BIGSERIAL      PRIMARY KEY,
    section          VARCHAR(100),
    row_name         VARCHAR(10)    NOT NULL,
    seat_number      VARCHAR(10)    NOT NULL,
    seat_type        VARCHAR(50)    NOT NULL,
    price_multiplier DECIMAL(5, 2)  NOT NULL,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    venue_id         BIGINT         NOT NULL REFERENCES venue(id),
    UNIQUE (venue_id, row_name, seat_number)
);
