package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.BookingItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Response for a single BookingItem within a BookingResponse (US-13 §6.1, §6.3).
//
// seatCode is computed as rowName + seatNumber (e.g. "A1", "B12").
// price is the immutable price captured at booking creation time.
// qrCode is NOT included — QR codes are generated after payment confirmation (US-14).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingItemResponse {

    private Long id;

    private Long seatId;

    // Computed seat display code: seat.rowName + seat.seatNumber (e.g. "A1").
    private String seatCode;

    // Immutable ticket price captured at booking creation: basePrice × priceMultiplier.
    private BigDecimal price;

    private BookingItemStatus status;
}
