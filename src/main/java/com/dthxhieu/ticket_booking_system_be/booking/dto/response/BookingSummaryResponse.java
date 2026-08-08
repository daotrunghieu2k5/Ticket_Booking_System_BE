package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Lightweight summary response for the booking list view (§6.2).
// Does not include items or payment details — fetch those via GET /api/v1/bookings/{id}.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryResponse {

    private Long id;

    private String bookingCode;

    private Long eventSessionId;

    private BookingStatus status;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;
}
