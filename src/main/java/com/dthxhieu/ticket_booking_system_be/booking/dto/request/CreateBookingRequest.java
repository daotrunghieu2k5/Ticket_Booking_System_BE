package com.dthxhieu.ticket_booking_system_be.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    // EventSession the user is booking for — validated in service (BR-02).
    @NotNull(message = "Event session ID is required")
    private Long eventSessionId;

    // SeatHold IDs from US-12 — represents the seats the user intends to book.
    // Duplicate and ownership checks are done in service (not here per §7 policy).
    @NotEmpty(message = "At least one seat hold ID is required")
    private List<Long> seatHoldIds;
}
