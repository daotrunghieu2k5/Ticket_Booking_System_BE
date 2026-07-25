package com.dthxhieu.ticket_booking_system_be.repository.auth;

import com.dthxhieu.ticket_booking_system_be.entity.auth.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository
        extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);
}