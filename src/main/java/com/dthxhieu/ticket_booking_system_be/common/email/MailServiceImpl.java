package com.dthxhieu.ticket_booking_system_be.common.email;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(MailSubject.VERIFY_EMAIL);
        message.setText(MailTemplate.verificationOtp(otp));
        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(MailSubject.RESET_PASSWORD);
        message.setText(MailTemplate.passwordResetOtp(otp));
        mailSender.send(message);
    }

}
