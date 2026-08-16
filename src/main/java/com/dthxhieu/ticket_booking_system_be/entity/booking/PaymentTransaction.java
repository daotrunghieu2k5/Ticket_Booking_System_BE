package com.dthxhieu.ticket_booking_system_be.entity.booking;

import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentTransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// PaymentTransaction entity per DATABASE.md §6.5.
//
// Each row represents one payment attempt (one call to PayOS).
// A single Payment may have multiple PaymentTransactions due to retries.
//
// orderCode strategy (US-14 §14):
//   orderCode = PaymentTransaction.id (BIGSERIAL Long).
//   transaction_code = String.valueOf(id) — used to look up the row from a webhook orderCode.
//   request_id = PayOS paymentLinkId — stored for audit and potential cancellation.
//
// gateway_payload stores the raw gateway JSON response for audit purposes.
// No checkout_url column — URL is returned directly in the API response and not persisted.
//
// Status transitions (no backward transitions allowed):
//   PENDING → SUCCESS (webhook success)
//   PENDING → FAILED  (webhook failure or amount mismatch)
//
// A FAILED transaction does NOT change Payment.status — Payment stays PENDING for retry.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Payment gateway name. Always "PAYOS" in US-14.
    @Column(nullable = false, length = 50)
    private String provider;

    // Maps to PaymentTransaction.id as a string.
    // Set AFTER first save so that id is available.
    // DB UNIQUE constraint enforces idempotency.
    @Column(name = "transaction_code", nullable = false, unique = true, length = 255)
    private String transactionCode;

    // PayOS paymentLinkId — returned by PayOS after creating the payment link.
    @Column(name = "request_id", length = 255)
    private String requestId;

    // Amount to collect. Must match Payment.amount and Booking.totalAmount.
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Currency code. Always "VND" in US-14.
    @Column(length = 10)
    private String currency;

    // Response code from PayOS webhook (e.g. "00" = success).
    @Column(name = "response_code", length = 50)
    private String responseCode;

    // Human-readable response message from PayOS webhook.
    @Column(name = "response_message", length = 500)
    private String responseMessage;

    // Raw JSON payload from PayOS webhook — audit only, not exposed in API responses.
    @Column(name = "gateway_payload", columnDefinition = "TEXT")
    private String gatewayPayload;

    // Transaction lifecycle status per PaymentTransactionStatus.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentTransactionStatus status;

    // Timestamp of the actual gateway transaction (from webhook transactionDateTime).
    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;

    // FK to Payment. Many transactions may belong to one Payment (due to retries).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
}
