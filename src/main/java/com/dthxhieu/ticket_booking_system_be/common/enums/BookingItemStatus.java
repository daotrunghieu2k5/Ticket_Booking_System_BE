package com.dthxhieu.ticket_booking_system_be.common.enums;

// BookingItem lifecycle status per DATABASE.md §12.
//
// VALID     — Ticket is valid and has not been used.
// USED      — Ticket was scanned at the event entry (future US).
// REFUNDED  — Ticket was refunded (future US).
// CANCELLED — Ticket was cancelled (future US).
//
// In US-13, all new BookingItems are created with VALID status.
public enum BookingItemStatus {
    VALID,
    USED,
    REFUNDED,
    CANCELLED
}
