package com.dthxhieu.ticket_booking_system_be.booking.service;

import vn.payos.model.webhooks.Webhook;

// PaymentWebhookService — processes incoming PayOS webhook payloads.
//
// Responsibilities:
//   1. Verify PayOS signature (delegates to PayOSService).
//   2. Find PaymentTransaction by orderCode.
//   3. Check idempotency — skip if transaction is not PENDING.
//   4. Validate amount.
//   5. Update PaymentTransaction / Payment / Booking atomically.
//   6. Clean up SeatHolds on SUCCESS.
public interface PaymentWebhookService {

    // Process a verified PayOS webhook payload.
    // The Webhook object contains the raw payload from PayOS.
    // Always completes without throwing to the caller (errors are logged, 200 returned).
    void processWebhook(Webhook webhook);
}
