package com.dthxhieu.ticket_booking_system_be.booking.service.impl;

import com.dthxhieu.ticket_booking_system_be.booking.dto.response.PaymentLinkResponse;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.PaymentResponse;
import com.dthxhieu.ticket_booking_system_be.booking.service.PaymentLocalTransactionService;
import com.dthxhieu.ticket_booking_system_be.booking.service.PaymentService;
import com.dthxhieu.ticket_booking_system_be.booking.service.PayOSService;
import com.dthxhieu.ticket_booking_system_be.common.enums.BookingStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentMethod;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentStatus;
import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ForbiddenException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.config.PayOSProperties;
import com.dthxhieu.ticket_booking_system_be.entity.booking.Booking;
import com.dthxhieu.ticket_booking_system_be.entity.booking.Payment;
import com.dthxhieu.ticket_booking_system_be.entity.booking.PaymentTransaction;
import com.dthxhieu.ticket_booking_system_be.repository.booking.BookingRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

// PaymentServiceImpl — orchestrates payment creation and retrieval.
//
// IMPORTANT: createPayment() is NOT annotated @Transactional.
// It calls PaymentLocalTransactionService for the DB commits (T1 and T2),
// with the external PayOS call between them — outside any DB transaction.
// This prevents holding a connection open during the HTTP round-trip to PayOS.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentLocalTransactionService localTxService;
    private final PayOSService payOSService;
    private final PayOSProperties payOSProperties;

    // ===========================================================================
    // CREATE PAYMENT
    // ===========================================================================

    // NOT @Transactional at the method level — transaction split is managed by
    // PaymentLocalTransactionService to avoid holding a DB connection open during
    // the external PayOS API call.
    @Override
    public PaymentLinkResponse createPayment(Long bookingId, Long userId) {

        // -----------------------------------------------------------------------
        // 1. Load Booking — 404 if not found.
        // -----------------------------------------------------------------------
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        // -----------------------------------------------------------------------
        // 2. Ownership check — 403 if booking belongs to another user.
        // -----------------------------------------------------------------------
        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to pay for this booking.");
        }

        // -----------------------------------------------------------------------
        // 3. Booking must be WAITING_PAYMENT — 400 otherwise.
        // -----------------------------------------------------------------------
        if (booking.getStatus() != BookingStatus.WAITING_PAYMENT) {
            throw new BusinessException("Booking is not in WAITING_PAYMENT status.");
        }

        // -----------------------------------------------------------------------
        // 4. Load Payment — 404 if not found.
        // -----------------------------------------------------------------------
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for this booking."));

        // -----------------------------------------------------------------------
        // 5. Payment must not already be SUCCESS — 400 if already paid.
        // -----------------------------------------------------------------------
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessException("This booking has already been paid.");
        }

        // -----------------------------------------------------------------------
        // 6. Set paymentMethod to PAYOS if not already set.
        //    (Payment was created with paymentMethod=null in US-13.)
        // -----------------------------------------------------------------------
        if (payment.getPaymentMethod() == null) {
            payment.setPaymentMethod(PaymentMethod.PAYOS);
            paymentRepository.save(payment);
        }

        // -----------------------------------------------------------------------
        // 7. T1 — Create PaymentTransaction(PENDING) and commit.
        //    ALWAYS create a new transaction; never reuse an old one.
        //    orderCode = transaction.id (set inside localTxService).
        // -----------------------------------------------------------------------
        PaymentTransaction pendingTransaction = localTxService.createPendingTransaction(payment.getId(), bookingId);
        long orderCode = pendingTransaction.getId();

        // -----------------------------------------------------------------------
        // 8. External PayOS call — outside any DB transaction.
        //    If this fails, T1 is already committed (PaymentTransaction = PENDING).
        //    The abandoned PENDING record is a valid audit trail.
        // -----------------------------------------------------------------------
        String description = "TT vé buổi #" + booking.getEventSession().getId();
        CreatePaymentLinkResponse gatewayResponse = payOSService.createPaymentLink(
                orderCode,
                payment.getAmount().longValue(),
                description,
                payOSProperties.getReturnUrl(),
                payOSProperties.getCancelUrl()
        );

        // -----------------------------------------------------------------------
        // 9. T2 — Save gateway response (paymentLinkId) to PaymentTransaction and commit.
        // -----------------------------------------------------------------------
        localTxService.updateTransactionWithGateway(pendingTransaction.getId(), gatewayResponse);

        log.info("[PaymentService] Payment link created. bookingId={}, transactionId={}, checkoutUrl={}",
                bookingId, pendingTransaction.getId(), gatewayResponse.getCheckoutUrl());

        return PaymentLinkResponse.builder()
                .paymentId(payment.getId())
                .bookingId(bookingId)
                .paymentMethod(PaymentMethod.PAYOS)
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .checkoutUrl(gatewayResponse.getCheckoutUrl())
                .build();
    }

    // ===========================================================================
    // GET PAYMENT BY ID
    // ===========================================================================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));

        // Ownership: verify via booking.user
        if (!payment.getBooking().getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to view this payment.");
        }

        return toPaymentResponse(payment);
    }

    // ===========================================================================
    // GET PAYMENT BY BOOKING ID
    // ===========================================================================

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId, Long userId) {
        // Load Booking first to verify ownership.
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to view this booking's payment.");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for this booking."));

        return toPaymentResponse(payment);
    }

    // ===========================================================================
    // PRIVATE HELPERS
    // ===========================================================================

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
