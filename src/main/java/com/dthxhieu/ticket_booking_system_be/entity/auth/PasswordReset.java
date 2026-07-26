package com.dthxhieu.ticket_booking_system_be.entity.auth;

import com.dthxhieu.ticket_booking_system_be.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Temporary record that holds the OTP for the forgot-password flow.
// There is deliberately NO foreign key to the users table (per spec section 8).
// This keeps the reset flow independent: even if the user row is deleted between
// forgot-password and reset-password requests, the record still exists and can be
// cleaned up without cascade issues.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset")
public class PasswordReset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One email -> at most one pending reset record (enforced by UNIQUE constraint in migration).
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String otpCode;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private Short attemptCount;
}
