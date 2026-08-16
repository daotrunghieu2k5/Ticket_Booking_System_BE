package com.dthxhieu.ticket_booking_system_be.booking.service.impl;

import com.dthxhieu.ticket_booking_system_be.booking.service.PayOSService;
import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

// PayOSServiceImpl — wraps PayOS SDK calls and translates SDK exceptions to project exceptions.
//
// SDK API used (payos-java:2.0.1):
//   payOS.paymentRequests().create(CreatePaymentLinkRequest) → CreatePaymentLinkResponse
//   payOS.webhooks().verifyPaymentWebhookData(Webhook)        → WebhookData
//
// No business logic here. Only gateway integration.
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSServiceImpl implements PayOSService {

    private final PayOS payOS;

    @Override
    public CreatePaymentLinkResponse createPaymentLink(
            long orderCode,
            long amount,
            String description,
            String returnUrl,
            String cancelUrl
    ) {
        // CreatePaymentLinkRequest uses builder pattern per SDK 2.0.1.
        // orderCode is Long (BIGSERIAL fits within PayOS 53-bit int limit).
        // amount is in VND (Long).
        CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        try {
            return payOS.paymentRequests().create(request);
        } catch (Exception e) {
            log.error("[PayOSService] Failed to create payment link. orderCode={}, error={}", orderCode, e.getMessage());
            throw new BusinessException("Failed to create payment link: " + e.getMessage());
        }
    }

    @Override
    public WebhookData verifyPaymentWebhookData(Object webhook) {
        // SDK method (payos-java:2.0.1): payOS.webhooks().verify(Object) → WebhookData
        // Accepts the raw Webhook object. Throws WebhookException on invalid signature.
        try {
            return payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            log.warn("[PayOSService] Webhook verification failed: {}", e.getMessage());
            throw new BusinessException("Invalid webhook signature.");
        }
    }
}
