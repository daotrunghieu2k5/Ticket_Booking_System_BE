-- V18: Drop NOT NULL constraint on booking_item.qr_code.
-- Rationale: US-13 creates BookingItems before payment is processed.
-- QR codes are generated only after successful payment (US-14).
-- The UNIQUE constraint on qr_code is preserved — when a qr_code is eventually
-- assigned, it must still be unique across all booking items.
ALTER TABLE booking_item ALTER COLUMN qr_code DROP NOT NULL;
