package com.dthxhieu.ticket_booking_system_be.booking.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentMethod;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// PaymentLinkResponse — returned by POST /api/v1/payments/{bookingId}.
//
// checkoutUrl is the PayOS redirect URL for the user to complete payment.
// The URL is not persisted in the database — it is generated fresh each call.
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkResponse {

    // Internal Payment ID.
    private Long paymentId;

    // Booking this payment is for.
    private Long bookingId;

    // Always PAYOS for US-14.
    private PaymentMethod paymentMethod;

    // Always PENDING at creation time (payment not yet confirmed).
    private PaymentStatus status;

    // Total amount to collect.
    private BigDecimal amount;

    // PayOS checkout URL — user must be redirected here to complete payment.
    private String checkoutUrl;
}
