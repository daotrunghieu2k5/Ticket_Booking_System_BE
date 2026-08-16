package com.dthxhieu.ticket_booking_system_be.common.enums;

// Payment lifecycle status per DATABASE.md §12.
//
// PENDING  — Payment record created, awaiting user action in payment gateway.
// SUCCESS  — Payment confirmed by payment gateway (US-14).
// FAILED   — Payment attempt failed in the gateway.
// REFUNDED — Payment was refunded (future US).
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
