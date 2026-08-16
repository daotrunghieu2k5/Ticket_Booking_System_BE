package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Compact booking summary for listing — GET /api/v1/bookings/me (US-13 §6.2).
// Does not include BookingItems or Payment detail.
// Clients use this for the bookings list view; they call GET /bookings/{id} for full detail.
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
