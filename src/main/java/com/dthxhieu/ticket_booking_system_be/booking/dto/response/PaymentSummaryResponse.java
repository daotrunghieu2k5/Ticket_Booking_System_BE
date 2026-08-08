package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentMethod;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Embedded in BookingResponse for create and detail views (§6.1, §6.3).
// paymentMethod is null when payment is created as PENDING (gateway not selected yet).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {

    private Long id;

    private BigDecimal amount;

    private PaymentStatus status;

    // Null until payment gateway is selected (future Payment US).
    private PaymentMethod paymentMethod;
}
