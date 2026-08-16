package com.dthxhieu.ticket_booking_system_be.booking.service.impl;

import com.dthxhieu.ticket_booking_system_be.booking.service.PaymentLocalTransactionService;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentTransactionStatus;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.booking.Payment;
import com.dthxhieu.ticket_booking_system_be.entity.booking.PaymentTransaction;
import com.dthxhieu.ticket_booking_system_be.repository.booking.PaymentRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.math.BigDecimal;

// PaymentLocalTransactionServiceImpl — manages the two DB transactions in the payment flow.
//
// Both methods use REQUIRES_NEW so each commits independently, ensuring:
//   T1 commits BEFORE the external PayOS API call starts.
//   T2 commits AFTER the PayOS API returns, updating the transaction record.
//
// By injecting this as a separate Spring bean (not self-invocation), Spring AOP proxy
// is always in play and @Transactional semantics are correctly enforced.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentLocalTransactionServiceImpl implements PaymentLocalTransactionService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    // T1: Create a new PaymentTransaction with status PENDING.
    //
    // Flow:
    //   1. Load Payment (to get the amount and payment entity reference).
    //   2. Build PaymentTransaction with PENDING status.
    //   3. Save once (to generate id).
    //   4. Set transactionCode = String.valueOf(id) — the orderCode sent to PayOS.
    //   5. Save again to persist transactionCode.
    //   6. COMMIT.
    //
    // transactionCode is set in two steps because we need the DB-generated id first.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTransaction createPendingTransaction(Long paymentId, Long bookingId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));

        // Build initial record — transactionCode is a placeholder until id is assigned.
        PaymentTransaction transaction = PaymentTransaction.builder()
                .provider("PAYOS")
                .transactionCode("PENDING_INIT")  // temporary; will be replaced after save
                .amount(payment.getAmount())
                .currency("VND")
                .status(PaymentTransactionStatus.PENDING)
                .payment(payment)
                .build();

        // First save to obtain the generated id.
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);

        // Now set transactionCode = String.valueOf(id) — this becomes the PayOS orderCode.
        saved.setTransactionCode(String.valueOf(saved.getId()));
        paymentTransactionRepository.save(saved);

        log.info("[PaymentLocalTx] Created PaymentTransaction id={} for paymentId={}", saved.getId(), paymentId);
        return saved;
    }

    // T2: Update PaymentTransaction with PayOS gateway response.
    //
    // Stores paymentLinkId in request_id for audit.
    // gateway_payload is not populated here — webhook will update with full response.
    // checkoutUrl is NOT persisted — it is returned directly to the API caller.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTransactionWithGateway(Long transactionId, CreatePaymentLinkResponse response) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction not found."));

        // paymentLinkId is the gateway's reference for this payment link (used for cancellation).
        transaction.setRequestId(response.getPaymentLinkId());

        paymentTransactionRepository.save(transaction);
        log.info("[PaymentLocalTx] Updated PaymentTransaction id={} with paymentLinkId={}", transactionId, response.getPaymentLinkId());
    }
}
