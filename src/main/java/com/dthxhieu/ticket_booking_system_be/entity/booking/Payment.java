package com.dthxhieu.ticket_booking_system_be.entity.booking;

import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentMethod;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentStatus;
import com.dthxhieu.ticket_booking_system_be.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Payment entity per DATABASE.md §6.4.
// Extends BaseEntity for created_at / updated_at (both present in the schema per DATABASE.md §6.4).
//
// One Booking → One Payment (UNIQUE(booking_id)).
// Payment history must NEVER be physically deleted (DATABASE.md §6.4 rule).
//
// US-13 creates Payment with status = PENDING and amount = Booking.totalAmount.
// payment_method is null at creation — gateway integration is a future US.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Amount must equal Booking.totalAmount at creation (BR-10).
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Null on creation — set when the user selects a payment gateway (future US).
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    // Initial status is PENDING on creation (BR-10).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    // FK → booking. UNIQUE(booking_id) enforced by DB constraint.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;
}
