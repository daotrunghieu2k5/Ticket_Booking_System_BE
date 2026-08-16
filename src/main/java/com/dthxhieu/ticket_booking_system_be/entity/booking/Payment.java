package com.dthxhieu.ticket_booking_system_be.entity.booking;

import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentMethod;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentStatus;
import com.dthxhieu.ticket_booking_system_be.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Payment entity per DATABASE.md §6.4.
//
// One Payment corresponds to exactly one Booking (enforced by UNIQUE on booking_id).
// Payment is created with status = PENDING when a Booking is created in US-13.
// The payment gateway flow (PayOS) is handled in US-14.
//
// Business rules:
//   - Payment amount must equal Booking.total_amount.
//   - Payment history must never be physically deleted.
//   - paymentMethod is null at creation time — set when user initiates gateway payment (US-14).
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

    // Total payment amount. Must equal Booking.totalAmount.
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Payment gateway. Null at creation — set when user selects a gateway in US-14.
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    // Payment lifecycle status. Starts at PENDING when Booking is created.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    // FK to Booking. UNIQUE enforced at DB level — one Payment per Booking.
    // FetchType.LAZY: we rarely need to load the full Booking when loading a Payment.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    // One Payment may have multiple PaymentTransactions due to retries (US-14).
    // CascadeType.ALL: transactions have no meaning without their Payment.
    // LAZY: never eager-load the full transaction history on every Payment load.
    @OneToMany(mappedBy = "payment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<PaymentTransaction> transactions = new ArrayList<>();
}
