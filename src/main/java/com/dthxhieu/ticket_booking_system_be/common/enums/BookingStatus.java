package com.dthxhieu.ticket_booking_system_be.common.enums;

// Booking lifecycle status per DATABASE.md §12.
//
// WAITING_PAYMENT — Booking created, Payment is PENDING. Awaiting payment gateway.
// PAID            — Payment confirmed successfully by the payment gateway (US-14).
// COMPLETED       — Booking fulfilled (e.g. event has occurred).
// CANCELLED       — Booking was cancelled (not physically deleted).
// EXPIRED         — Booking expired because payment was not completed in time.
public enum BookingStatus {
    WAITING_PAYMENT,
    PAID,
    COMPLETED,
    CANCELLED,
    EXPIRED
}

