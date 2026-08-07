-- V11 created seat_number as VARCHAR(10).
-- The spec (US-09) requires seat_number to be an INTEGER:
--   - BR-03: "Seat number must be greater than zero"
--   - API contract uses numeric values (e.g. "seatNumber": 1)
--   - Spring Data JPA derived query uses Integer parameter
-- This migration converts the column type without touching the completed V11.
ALTER TABLE seat
    ALTER COLUMN seat_number TYPE INTEGER USING seat_number::INTEGER;
