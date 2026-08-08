package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.SeatAvailabilityStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Response item for one seat in the seat map (US-12 §6.1).
// price = eventSession.basePrice × seat.priceMultiplier, rounded HALF_UP to 2 decimal places.
// This is the basis for BookingItem.price in US-13.
// status is computed from SeatHold and BookingItem data — NOT stored in the Seat table.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailabilityItem {

    private Long id;

    // Computed display code (rowName + seatNumber, e.g. "A1", "B12").
    private String seatCode;

    private String rowName;

    private SeatType seatType;

    // Final ticket price for this seat in this session.
    // Formula: eventSession.basePrice × seat.priceMultiplier (HALF_UP, scale 2).
    private BigDecimal price;

    // AVAILABLE, HELD, or BOOKED — view-layer only, computed by service.
    private SeatAvailabilityStatus status;
}
