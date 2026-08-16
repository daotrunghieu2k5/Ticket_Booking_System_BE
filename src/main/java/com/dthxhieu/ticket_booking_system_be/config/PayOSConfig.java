package com.dthxhieu.ticket_booking_system_be.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

// PayOSConfig creates the PayOS SDK client bean.
//
// PayOS is a stateless client — constructed once as a singleton bean.
// Constructor: PayOS(clientId, apiKey, checksumKey) per SDK docs.
// All credentials are injected via PayOSProperties (from environment variables).
@Configuration
@EnableConfigurationProperties(PayOSProperties.class)
public class PayOSConfig {

    @Bean
    public PayOS payOS(PayOSProperties props) {
        return new PayOS(props.getClientId(), props.getApiKey(), props.getChecksumKey());
    }
}
