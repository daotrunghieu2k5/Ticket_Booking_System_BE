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

    public static String passwordResetOtp(String otp) {

        return """
                Hello,

                We received a request to reset your password.

                Your password reset code is:

                %s

                This code will expire in 5 minutes.

                If you did not request a password reset, please ignore this email.

                Ticket Booking System
                """.formatted(otp);
    }

}