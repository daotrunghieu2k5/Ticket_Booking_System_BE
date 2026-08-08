package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.BookingItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Response for a single BookingItem — used inside BookingResponse (§6.1, §6.3).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingItemResponse {

    private Long id;

    // The seat's DB primary key.
    private Long seatId;

    // Human-readable seat code (rowName + seatNumber, e.g. "A01").
    private String seatCode;

    // Immutable price stored at booking time: basePrice × priceMultiplier (BR-06).
    private BigDecimal price;

    private BookingItemStatus status;
}
