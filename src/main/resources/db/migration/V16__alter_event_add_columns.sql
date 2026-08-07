-- Additive migration: adds columns required by US-10 Event Management spec.
-- DATABASE.md §5.4 only listed core columns; these three extend the event table
-- to support the business requirements defined in US-10.
--
-- banner     — optional banner image URL (BR-08: nullable).
-- sale_start_time — ticket sale opening time (BR-09: must be before sale_end_time).
-- sale_end_time   — ticket sale closing time.
ALTER TABLE event
    ADD COLUMN banner          VARCHAR(500),
    ADD COLUMN sale_start_time TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN sale_end_time   TIMESTAMP WITHOUT TIME ZONE;
