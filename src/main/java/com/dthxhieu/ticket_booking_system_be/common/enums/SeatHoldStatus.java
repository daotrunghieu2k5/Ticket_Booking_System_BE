package com.dthxhieu.ticket_booking_system_be.common.enums;

// SeatHold lifecycle status per DATABASE.md §6.1 and US-12.
//
// ACTIVE    — hold was created and has not yet expired.
//             The partial unique index (WHERE status = 'ACTIVE') enforces
//             at most one ACTIVE hold per (event_session_id, seat_id).
// EXPIRED   — hold was ACTIVE but expired_at has passed.
//             Set lazily (on next hold attempt) — no scheduled job in US-12.
//             Excluded from the partial unique index → does not block new holds.
// CANCELLED — user released the hold before it expired.
//             Excluded from the partial unique index → seat immediately re-holdable.
public enum SeatHoldStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
}
