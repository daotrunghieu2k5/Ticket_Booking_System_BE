package com.dthxhieu.ticket_booking_system_be.common.enums;

// Supported seat types per US-09 BR-05.
// Stored as VARCHAR in the database per project enum convention.
// Future seat types can be added here without changing existing business logic.
public enum SeatType {
    STANDARD,
    VIP,
    COUPLE,
    WHEELCHAIR
}
