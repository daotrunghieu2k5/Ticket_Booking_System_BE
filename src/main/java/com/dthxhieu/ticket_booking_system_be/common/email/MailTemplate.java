package com.dthxhieu.ticket_booking_system_be.common.email;

public final class MailTemplate {

    private MailTemplate() {
    }

    public static String verificationOtp(String otp) {

        return """
                Hello,

                Thank you for registering an account.

                Your verification code is:

                %s

                This code will expire in 5 minutes.

                If you did not request this email, please ignore it.

                Ticket Booking System
                """.formatted(otp);
    }

}