package com.dthxhieu.ticket_booking_system_be.venue.dto.request;

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
public class BatchDeleteSeatRequest {

    // At least one seat ID must be provided. Duplicate ID check is done in service.
    @NotEmpty(message = "Seat IDs must not be empty")
    private List<Long> seatIds;
}
