package com.dthxhieu.ticket_booking_system_be.repository.auth;

import com.dthxhieu.ticket_booking_system_be.entity.auth.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);
}
