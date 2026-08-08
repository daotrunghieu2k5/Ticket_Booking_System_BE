package com.dthxhieu.ticket_booking_system_be.event.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Unified response DTO for both list (§6.1) and detail (§6.2) views.
// eventId and venueId are flat IDs per the spec contract — no nested objects needed.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSessionResponse {

    private Long id;

    private Long eventId;

    private Long venueId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime bookingOpen;

    private LocalDateTime bookingClose;

    private BigDecimal basePrice;

    private SessionStatus status;
}
