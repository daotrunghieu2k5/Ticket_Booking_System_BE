package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentMethod;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Summary of a Payment included in BookingResponse (US-13 §6.1, §6.3).
//
// In US-13, status = PENDING and paymentMethod = null (no gateway selected yet).
// US-14 will update paymentMethod and status via the PayOS callback.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {

    private Long id;

    private BigDecimal amount;

    private PaymentStatus status;

    // Null until user initiates a specific payment gateway (US-14).
    private PaymentMethod paymentMethod;
}
