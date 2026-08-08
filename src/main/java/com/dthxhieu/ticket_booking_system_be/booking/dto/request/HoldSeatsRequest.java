package com.dthxhieu.ticket_booking_system_be.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
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
public class HoldSeatsRequest {

    // At least one seat must be requested (US-12 §7.1).
    // Duplicate seatId check is done in service — Jakarta Validation cannot deduplicate a list.
    @NotEmpty(message = "At least one seat ID is required")
    private List<Long> seatIds;
}
