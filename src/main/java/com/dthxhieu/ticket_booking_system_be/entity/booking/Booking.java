package com.dthxhieu.ticket_booking_system_be.entity.booking;

import com.dthxhieu.ticket_booking_system_be.common.enums.BookingStatus;
import com.dthxhieu.ticket_booking_system_be.entity.BaseEntity;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
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
import jakarta.persistence.ManyToOne;
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

// Booking entity per DATABASE.md §6.2.
//
// One Booking belongs to one User and one EventSession.
// A Booking must contain at least one BookingItem (enforced by Service layer).
// Booking history must never be physically deleted (enforced by Service layer — no delete operations).
//
// Status lifecycle:
//   WAITING_PAYMENT → Payment created, awaiting gateway (US-13).
//   PAID            → Payment confirmed (US-14).
//   COMPLETED       → Event occurred.
//   CANCELLED       → Booking cancelled.
//   EXPIRED         → Payment window expired.
//
// Cascade strategy per DATABASE.md §10:
//   Booking → BookingItem: CascadeType.ALL (child items have no meaning without booking)
//   Booking → Payment: CascadeType.ALL (payment has no meaning without booking)
//   User → Booking: NONE (user deletion does not cascade to booking history)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking")
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique human-readable booking reference. Format: BK-{yyyyMMdd}-{6_alphanum}.
    // Database UNIQUE constraint enforces uniqueness as the final safety net.
    @Column(name = "booking_code", nullable = false, unique = true, length = 50)
    private String bookingCode;

    // Total ticket price = SUM(bookingItem.price). Calculated by Service — never trusted from client.
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    // Booking lifecycle status stored as VARCHAR via @Enumerated(STRING).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BookingStatus status;

    // Owner of the booking. NONE cascade: User deletion must not remove booking history.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The session this booking is for.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;

    // BookingItems are owned by this Booking. CascadeType.ALL per DATABASE.md §10.
    // mappedBy = "booking" because BookingItem.booking is the owning side.
    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingItem> items = new ArrayList<>();

    // One Booking has exactly one Payment. CascadeType.ALL per DATABASE.md §10.
    // mappedBy = "booking" because Payment.booking is the owning side (holds the FK).
    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;
}
