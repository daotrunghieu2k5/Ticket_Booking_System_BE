package com.dthxhieu.ticket_booking_system_be.booking.service;

import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

// PayOSService — isolates all communication with the PayOS gateway.
//
// Business logic (Booking / Payment / SeatHold) must NOT be placed here.
// All SDK exceptions are caught inside the implementation and re-thrown as
// BusinessException so that the caller does not depend on SDK-specific exception types.
public interface PayOSService {

    // Create a payment link on PayOS for the given order.
    // orderCode MUST be the PaymentTransaction.id (Long).
    // amount is in VND (already as long; SDK expects long).
    // Returns CreatePaymentLinkResponse which contains checkoutUrl and paymentLinkId.
    CreatePaymentLinkResponse createPaymentLink(
            long orderCode,
            long amount,
            String description,
            String returnUrl,
            String cancelUrl
    );

    // Verify and parse a PayOS webhook payload.
    // Accepts the raw Webhook object (vn.payos.model.webhooks.Webhook).
    // SDK method: payOS.webhooks().verify(Object) → WebhookData
    // Throws BusinessException if signature is invalid.
    WebhookData verifyPaymentWebhookData(Object webhook);
}
