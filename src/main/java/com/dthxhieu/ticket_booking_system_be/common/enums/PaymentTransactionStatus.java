package com.dthxhieu.ticket_booking_system_be.common.enums;

// PaymentTransactionStatus — lifecycle of a single payment attempt to the gateway.
//
// PENDING  — Transaction created locally; gateway call has not completed yet.
// SUCCESS  — Gateway confirmed payment was received.
// FAILED   — Gateway reported failure, or amount mismatch detected on webhook.
//
// Note: A failed transaction does NOT affect Payment.status (which remains PENDING).
// This allows the user to retry by initiating a new PaymentTransaction.
public enum PaymentTransactionStatus {
    PENDING,
    SUCCESS,
    FAILED
}
