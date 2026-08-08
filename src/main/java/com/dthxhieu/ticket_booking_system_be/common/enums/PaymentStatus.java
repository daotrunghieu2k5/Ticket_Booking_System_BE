package com.dthxhieu.ticket_booking_system_be.common.enums;

// Payment lifecycle status per DATABASE.md §12.
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
