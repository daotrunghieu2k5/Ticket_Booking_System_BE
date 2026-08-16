package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

// Response for a successful seat hold operation (US-12 §6.2) and get-my-hold (§6.4).
// holdId represents the first SeatHold record's PK — one row per seat is created
// but the response groups them under a single hold reference for the client.
// seatHoldIds contains ALL SeatHold PKs created in the same request so that the
// frontend can pass the full list to CreateBookingRequest.seatHoldIds.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldResponse {

    // First SeatHold record's PK — kept for backward compatibility.
    private Long holdId;

    // All SeatHold PKs created in this hold operation (one per seat).
    private List<Long> seatHoldIds;

    private Long eventSessionId;

    // All seat IDs that were held in this operation.
    private List<Long> seatIds;

    // Expiration time — same for all seats in the same hold request.
    private LocalDateTime expiresAt;
}
