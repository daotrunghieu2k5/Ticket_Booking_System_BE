package com.dthxhieu.ticket_booking_system_be.common.enums;

// View-layer status for a seat within a specific EventSession.
// NOT stored in the database — computed on-the-fly from SeatHold and BookingItem data.
//
// Derivation logic (US-12 BR-02):
//   BOOKED    → a valid BookingItem exists for this (seat, eventSession).
//   HELD      → an ACTIVE SeatHold exists with expired_at > now.
//   AVAILABLE → neither of the above conditions is true.
public enum SeatAvailabilityStatus {
    AVAILABLE,
    HELD,
    BOOKED
}
