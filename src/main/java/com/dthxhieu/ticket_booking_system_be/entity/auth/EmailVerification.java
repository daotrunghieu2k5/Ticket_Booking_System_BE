package com.dthxhieu.ticket_booking_system_be.entity.auth;

import com.dthxhieu.ticket_booking_system_be.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone")
    private String phone;

    @Column(name = "otp_code", length = 6, nullable = false)
    private String otpCode;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "attempt_count", nullable = false)
    private Short attemptCount = 0;

}
