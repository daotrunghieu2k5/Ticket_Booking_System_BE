package com.dthxhieu.ticket_booking_system_be.booking.controller;

import com.dthxhieu.ticket_booking_system_be.booking.service.PaymentWebhookService;
import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.Webhook;

// PaymentWebhookController — public endpoint for PayOS webhooks (US-14).
//
// POST /api/v1/payments/webhook/payos is permitAll() in SecurityConfig.
// The controller always returns HTTP 200 to acknowledge receipt.
// PayOS signature verification is performed inside PaymentWebhookService.
//
// Security:
//   - Signature verification is performed inside PaymentWebhookServiceImpl.
//   - Invalid signatures result in no DB changes (log + return 200).
//   - Duplicate webhooks are idempotent (no-op if transaction not PENDING).
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    // POST /api/v1/payments/webhook/payos
    // Receives PayOS payment webhook. Always returns 200 OK to prevent PayOS retry storms.
    // Actual processing (signature verification, state update) is in PaymentWebhookService.
    @PostMapping("/payos")
    public ResponseEntity<ApiResponse<Void>> handlePayOSWebhook(
            @RequestBody Webhook webhook
    ) {
        log.info("[Webhook] Received PayOS webhook.");
        try {
            paymentWebhookService.processWebhook(webhook);
        } catch (Exception e) {
            // Log unexpected errors but still return 200 to PayOS.
            // Returning 4xx/5xx would cause PayOS to retry infinitely.
            log.error("[Webhook] Unexpected error during webhook processing: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Webhook received.")
                .build());
    }
}
