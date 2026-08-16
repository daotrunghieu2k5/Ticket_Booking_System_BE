package com.dthxhieu.ticket_booking_system_be.repository.booking;

import com.dthxhieu.ticket_booking_system_be.entity.booking.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// PaymentTransactionRepository — US-14.
//
// findByTransactionCode is the primary lookup method for webhook processing.
// Webhook arrives with orderCode (= PaymentTransaction.id) → convert to String → find row.
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    // Look up by transaction_code = String.valueOf(orderCode).
    // Used in webhook processing to find the local PaymentTransaction from the PayOS orderCode.
    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);
}
