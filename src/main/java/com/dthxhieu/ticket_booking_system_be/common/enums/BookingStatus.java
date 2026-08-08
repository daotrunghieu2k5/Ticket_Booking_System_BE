package com.dthxhieu.ticket_booking_system_be.common.enums;

// Booking lifecycle status per DATABASE.md §12.
public enum BookingStatus {
    PENDING,
    WAITING_PAYMENT,
    PAID,
    COMPLETED,
    CANCELLED,
    EXPIRED
}
