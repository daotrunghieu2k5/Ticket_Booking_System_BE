package com.dthxhieu.ticket_booking_system_be.event.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventSessionRequest {

    // FR-08: Must reference an existing Event. Validated in service.
    @NotNull(message = "Event ID is required")
    private Long eventId;

    // FR-09: Must reference an existing Venue. Validated in service.
    @NotNull(message = "Venue ID is required")
    private Long venueId;

    // FR-10: startTime < endTime validated in service (cross-field, belongs to Service per spec §7).
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    // FR-11: bookingOpen < bookingClose < startTime validated in service.
    @NotNull(message = "Booking open time is required")
    private LocalDateTime bookingOpen;

    @NotNull(message = "Booking close time is required")
    private LocalDateTime bookingClose;

    // BR-07: Base price must be > 0.
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than zero")
    private BigDecimal basePrice;
}
