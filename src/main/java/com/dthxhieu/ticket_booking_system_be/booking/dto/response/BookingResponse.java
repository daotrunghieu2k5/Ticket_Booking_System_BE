package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Full booking detail response.
// Used for:
//   - POST /api/v1/bookings (create booking, returns created booking detail)
//   - GET  /api/v1/bookings/{id} (retrieve booking detail)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;

    private String bookingCode;

    private Long eventSessionId;

    private BookingStatus status;

    private BigDecimal totalAmount;

    // Full list of BookingItems (seats purchased).
    private List<BookingItemResponse> items;

    // Embedded payment summary.
    private PaymentSummaryResponse payment;

    private LocalDateTime createdAt;
}
