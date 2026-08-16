package com.dthxhieu.ticket_booking_system_be.booking.service.impl;

import com.dthxhieu.ticket_booking_system_be.booking.service.PayOSService;
import com.dthxhieu.ticket_booking_system_be.booking.service.PaymentWebhookService;
import com.dthxhieu.ticket_booking_system_be.common.enums.BookingStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentTransactionStatus;
import com.dthxhieu.ticket_booking_system_be.entity.booking.Booking;
import com.dthxhieu.ticket_booking_system_be.entity.booking.Payment;
import com.dthxhieu.ticket_booking_system_be.entity.booking.PaymentTransaction;
import com.dthxhieu.ticket_booking_system_be.repository.booking.BookingItemRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.PaymentTransactionRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.SeatHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// PaymentWebhookServiceImpl — processes PayOS webhook callbacks.
//
// Webhook SUCCESS flow:
//   Verify signature → find transaction → idempotency check → amount check →
//   if Payment already SUCCESS: mark tx SUCCESS only (guard for concurrent transactions) →
//   else: update tx/Payment/Booking → delete SeatHolds (scoped) → COMMIT
//
// Webhook FAILED flow:
//   Verify signature → find transaction → idempotency check →
//   mark tx FAILED → Payment stays PENDING → Booking stays WAITING_PAYMENT → COMMIT
//
// All DB updates are atomic within one @Transactional.
// No external API calls inside this transaction.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    // PayOS success code per gateway documentation.
    private static final String PAYOS_SUCCESS_CODE = "00";

    private final PayOSService payOSService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingItemRepository bookingItemRepository;
    private final SeatHoldRepository seatHoldRepository;

    @Override
    @Transactional
    public void processWebhook(Webhook webhook) {

        // -----------------------------------------------------------------------
        // Step 1 — Verify PayOS signature.
        // If invalid, log and return. No DB changes.
        // -----------------------------------------------------------------------
        WebhookData webhookData;
        try {
            webhookData = payOSService.verifyPaymentWebhookData(webhook);
        } catch (Exception e) {
            log.warn("[Webhook] Signature verification failed: {}", e.getMessage());
            return;
        }

        Long orderCode = webhookData.getOrderCode();
        String transactionCode = String.valueOf(orderCode);

        // -----------------------------------------------------------------------
        // Step 2 — Find PaymentTransaction by transactionCode.
        // If not found, log and return (could be from another system).
        // -----------------------------------------------------------------------
        PaymentTransaction transaction = paymentTransactionRepository
                .findByTransactionCode(transactionCode)
                .orElse(null);

        if (transaction == null) {
            log.warn("[Webhook] PaymentTransaction not found for orderCode={}", orderCode);
            return;
        }

        // -----------------------------------------------------------------------
        // Step 3 — Idempotency check.
        // If transaction is no longer PENDING, it was already processed.
        // -----------------------------------------------------------------------
        if (transaction.getStatus() != PaymentTransactionStatus.PENDING) {
            log.info("[Webhook] Idempotency: transaction {} already in status {}", transactionCode, transaction.getStatus());
            return;
        }

        // -----------------------------------------------------------------------
        // Step 4 — Amount validation.
        // webhook amount must match the transaction amount.
        // -----------------------------------------------------------------------
        Payment payment = transaction.getPayment();
        BigDecimal webhookAmount = BigDecimal.valueOf(webhookData.getAmount());

        if (transaction.getAmount().compareTo(webhookAmount) != 0) {
            log.warn("[Webhook] Amount mismatch for orderCode={}. expected={}, got={}",
                    orderCode, transaction.getAmount(), webhookAmount);
            // Mark transaction as FAILED — Payment remains PENDING, user can retry.
            updateTransactionStatus(transaction, PaymentTransactionStatus.FAILED, webhookData);
            return;
        }

        boolean isSuccess = PAYOS_SUCCESS_CODE.equals(webhookData.getCode());

        if (isSuccess) {
            handleSuccess(transaction, payment, webhookData);
        } else {
            handleFailure(transaction, webhookData);
        }
    }

    // -----------------------------------------------------------------------
    // SUCCESS path.
    // Guard: if Payment is already SUCCESS (e.g. another transaction was processed first),
    // only update this transaction's status — do NOT update Booking or delete SeatHolds again.
    // -----------------------------------------------------------------------
    private void handleSuccess(PaymentTransaction transaction, Payment payment, WebhookData webhookData) {
        updateTransactionStatus(transaction, PaymentTransactionStatus.SUCCESS, webhookData);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            // Another transaction already completed this payment — idempotency guard at Payment level.
            log.info("[Webhook] Payment {} already SUCCESS. Transaction {} marked SUCCESS (audit only).",
                    payment.getId(), transaction.getId());
            return;
        }

        // First SUCCESS for this Payment — update Payment and Booking.
        payment.setStatus(PaymentStatus.SUCCESS);

        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.PAID);

        log.info("[Webhook] Payment SUCCESS. paymentId={}, bookingId={}", payment.getId(), booking.getId());

        // -----------------------------------------------------------------------
        // SeatHold cleanup — scoped to: userId + eventSessionId + seatIds from BookingItems.
        // MUST NOT delete SeatHolds of other users or other sessions.
        // If SeatHolds were already expired/deleted, this is a no-op.
        // -----------------------------------------------------------------------
        Long userId = booking.getUser().getId();
        Long eventSessionId = booking.getEventSession().getId();

        List<Long> seatIds = bookingItemRepository.findByBookingId(booking.getId())
                .stream()
                .map(item -> item.getSeat().getId())
                .toList();

        if (!seatIds.isEmpty()) {
            seatHoldRepository.deleteByUserIdAndEventSessionIdAndSeatIdIn(userId, eventSessionId, seatIds);
            log.info("[Webhook] SeatHolds deleted for userId={}, eventSessionId={}, seatIds={}", userId, eventSessionId, seatIds);
        }
    }

    // -----------------------------------------------------------------------
    // FAILED path.
    // Only the PaymentTransaction is marked FAILED.
    // Payment stays PENDING → user can retry.
    // Booking stays WAITING_PAYMENT.
    // -----------------------------------------------------------------------
    private void handleFailure(PaymentTransaction transaction, WebhookData webhookData) {
        updateTransactionStatus(transaction, PaymentTransactionStatus.FAILED, webhookData);
        log.info("[Webhook] Payment attempt FAILED for transactionId={}. Payment remains PENDING for retry.",
                transaction.getId());
    }

    // -----------------------------------------------------------------------
    // Helper: update transaction status and gateway fields.
    // -----------------------------------------------------------------------
    private void updateTransactionStatus(
            PaymentTransaction transaction,
            PaymentTransactionStatus status,
            WebhookData webhookData
    ) {
        transaction.setStatus(status);
        transaction.setResponseCode(webhookData.getCode());
        transaction.setResponseMessage(webhookData.getDesc());
        transaction.setGatewayPayload(webhookData.toString());

        // Parse transactionDateTime from gateway (format may vary — store safely).
        try {
            if (webhookData.getTransactionDateTime() != null) {
                transaction.setTransactionTime(
                        LocalDateTime.parse(webhookData.getTransactionDateTime(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                );
            }
        } catch (Exception e) {
            log.warn("[Webhook] Could not parse transactionDateTime: {}", webhookData.getTransactionDateTime());
        }

        paymentTransactionRepository.save(transaction);
    }
}
