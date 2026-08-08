package com.dthxhieu.ticket_booking_system_be.common.enums;

// EventSession lifecycle statuses per US-11 BR-09.
// Do not invent additional statuses — use only these five values.
//
// UPCOMING       — session is scheduled but booking has not opened yet.
// BOOKING_OPEN   — booking window is currently open.
// BOOKING_CLOSED — booking window has closed; session has not started yet.
// FINISHED       — session has ended.
// CANCELLED      — session was cancelled; must not be physically deleted.
public enum SessionStatus {
    UPCOMING,
    BOOKING_OPEN,
    BOOKING_CLOSED,
    FINISHED,
    CANCELLED
}
