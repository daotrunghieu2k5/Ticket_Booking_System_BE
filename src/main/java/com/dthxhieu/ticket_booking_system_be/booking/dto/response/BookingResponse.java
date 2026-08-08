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

// Full booking response for create (§6.1) and detail (§6.3) views.
// Includes BookingItems and embedded Payment summary.
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

    private List<BookingItemResponse> items;

    private PaymentSummaryResponse payment;

    private LocalDateTime createdAt;
}
