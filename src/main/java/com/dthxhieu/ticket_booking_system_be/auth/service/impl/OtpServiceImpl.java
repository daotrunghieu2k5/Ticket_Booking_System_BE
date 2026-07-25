package com.dthxhieu.ticket_booking_system_be.auth.service.impl;

import com.dthxhieu.ticket_booking_system_be.auth.service.OtpService;
import com.dthxhieu.ticket_booking_system_be.common.email.MailConstant;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    @Override
    public LocalDateTime generateExpiredAt() {
        return LocalDateTime.now()
                .plusMinutes(MailConstant.OTP_EXPIRE_MINUTES);
    }
}