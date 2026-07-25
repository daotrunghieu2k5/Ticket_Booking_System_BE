package com.dthxhieu.ticket_booking_system_be.auth.service;

import java.time.LocalDateTime;

public interface OtpService {

    String generateOtp();

    LocalDateTime generateExpiredAt();

}