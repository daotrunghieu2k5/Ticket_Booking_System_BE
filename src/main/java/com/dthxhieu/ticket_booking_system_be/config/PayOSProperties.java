package com.dthxhieu.ticket_booking_system_be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

// PayOS configuration properties.
//
// All values are read from environment variables — no credentials are hard-coded.
// Required environment variables:
//   PAYOS_CLIENT_ID
//   PAYOS_API_KEY
//   PAYOS_CHECKSUM_KEY
//   PAYOS_RETURN_URL   — frontend URL to redirect to after payment
//   PAYOS_CANCEL_URL   — frontend URL to redirect to if user cancels
@ConfigurationProperties(prefix = "payos")
@Getter
@Setter
public class PayOSProperties {

    // payOS client ID. Maps from PAYOS_CLIENT_ID via ${PAYOS_CLIENT_ID}.
    private String clientId;

    // payOS API key. Maps from PAYOS_API_KEY via ${PAYOS_API_KEY}.
    private String apiKey;

    // payOS checksum key used to verify webhook signatures.
    // Maps from PAYOS_CHECKSUM_KEY via ${PAYOS_CHECKSUM_KEY}.
    private String checksumKey;

    // Frontend URL for payment success redirect. Maps from PAYOS_RETURN_URL.
    private String returnUrl;

    // Frontend URL for payment cancel redirect. Maps from PAYOS_CANCEL_URL.
    private String cancelUrl;
}
