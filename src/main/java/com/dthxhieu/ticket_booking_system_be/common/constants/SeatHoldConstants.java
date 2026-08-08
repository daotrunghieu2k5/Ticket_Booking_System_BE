package com.dthxhieu.ticket_booking_system_be.common.constants;

// Shared constants for SeatHold business rules (US-12 BR-04).
//
// BR-04: The hold duration must use a project-defined constant.
// Do not hard-code hold durations in different services.
//
// HOLD_DURATION_MINUTES is the single source of truth for how long a SeatHold
// remains ACTIVE before it is considered expired.
public final class SeatHoldConstants {

    // Duration a SeatHold is considered ACTIVE after creation.
    public static final int HOLD_DURATION_MINUTES = 10;

    // Prevent instantiation — this is a utility constants class.
    private SeatHoldConstants() {}
}
