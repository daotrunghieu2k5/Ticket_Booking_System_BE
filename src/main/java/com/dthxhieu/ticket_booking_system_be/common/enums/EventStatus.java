package com.dthxhieu.ticket_booking_system_be.common.enums;

// Event lifecycle status per US-10 BR-10.
// ACTIVE  — event is visible to public and available for booking.
// INACTIVE — event is hidden from public; used for soft-delete when sessions exist (BR-13).
public enum EventStatus {
    ACTIVE,
    INACTIVE
}
