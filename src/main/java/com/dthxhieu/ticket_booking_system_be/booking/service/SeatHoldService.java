package com.dthxhieu.ticket_booking_system_be.booking.service;

import com.dthxhieu.ticket_booking_system_be.booking.dto.request.HoldSeatsRequest;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.SeatAvailabilityItem;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.SeatHoldResponse;

import java.util.List;

public interface SeatHoldService {

    // Returns the seat map for an EventSession — public (FR-01, FR-02).
    // Uses 3 batch queries to compute status without N+1 (see implementation).
    List<SeatAvailabilityItem> getSeatMap(Long sessionId);

    // Atomically holds one or more seats for the authenticated user (FR-07, FR-08, BR-06).
    SeatHoldResponse holdSeats(Long sessionId, HoldSeatsRequest request, Long userId);

    // Releases a specific hold — authenticated, owner only (FR-14, BR-05).
    void releaseHold(Long holdId, Long userId);

    // Returns the current user's active hold for a specific session (§6.4).
    SeatHoldResponse getMyActiveHold(Long sessionId, Long userId);
}
