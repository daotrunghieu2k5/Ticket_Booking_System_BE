package com.dthxhieu.ticket_booking_system_be.common.email;

public interface MailService {

    void sendOtpEmail(String email, String otp);

    void sendPasswordResetOtpEmail(String email, String otp);

}