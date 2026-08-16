package com.dthxhieu.ticket_booking_system_be.booking.service;

import com.dthxhieu.ticket_booking_system_be.booking.dto.response.PaymentLinkResponse;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.PaymentResponse;

// PaymentService — business logic for payment operations.
//
// createPayment: create a new PaymentTransaction and return a checkout URL.
// getPaymentById: retrieve payment details by payment ID (ownership enforced).
// getPaymentByBookingId: retrieve payment details by booking ID (ownership enforced).
public interface PaymentService {

    PaymentLinkResponse createPayment(Long bookingId, Long userId);

    PaymentResponse getPaymentById(Long paymentId, Long userId);

    PaymentResponse getPaymentByBookingId(Long bookingId, Long userId);
}
