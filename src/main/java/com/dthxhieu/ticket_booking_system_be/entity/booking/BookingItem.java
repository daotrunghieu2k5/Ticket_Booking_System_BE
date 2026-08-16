package com.dthxhieu.ticket_booking_system_be.entity.booking;

import com.dthxhieu.ticket_booking_system_be.common.enums.BookingItemStatus;
import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// BookingItem entity per DATABASE.md §6.3.
//
// One BookingItem represents one purchased seat in one EventSession.
//
// Key rules:
//   - UNIQUE(event_session_id, seat_id): one seat can only be sold once per session.
//     This DB constraint is the final concurrency protection against race conditions.
//   - price is immutable — calculated at booking creation time and never updated.
//   - qrCode is NULL in US-13. QR codes are generated after payment confirmation (US-14).
//   - event_session_id must always equal booking.event_session_id (invariant enforced by Service).
//   - BookingItem does NOT extend BaseEntity — DATABASE.md §6.3 has no audit columns.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "booking_item",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"qr_code"}),
                @UniqueConstraint(columnNames = {"event_session_id", "seat_id"})
        }
)
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // QR code for ticket scanning. NULL until payment is confirmed (US-14).
    // Column was made nullable via V18 migration. UNIQUE constraint is preserved.
    @Column(name = "qr_code", unique = true, length = 255)
    private String qrCode;

    // Final ticket price at booking time. Formula: eventSession.basePrice × seat.priceMultiplier.
    // Immutable — stored as a snapshot; future price changes on Seat/Session do not affect this.
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    // Ticket status. VALID on creation; USED/REFUNDED/CANCELLED managed by future USs.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BookingItemStatus status;

    // Immutable snapshot of ticket info stored as JSON text.
    // Null in US-13 — populated in future ticket generation US.
    @Column(name = "event_snapshot", columnDefinition = "TEXT")
    private String eventSnapshot;

    // FK to Booking. Owning side of Booking → BookingItem relationship.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // FK to Seat. The physical seat that was purchased.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // FK to EventSession. Denormalized for the UNIQUE(event_session_id, seat_id) constraint.
    // Must always equal booking.eventSession.id — enforced by BookingServiceImpl.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;
}
