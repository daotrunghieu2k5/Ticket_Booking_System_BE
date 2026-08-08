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
// Represents one purchased seat (one BookingItem = one Seat for one EventSession).
//
// BookingItem does NOT extend BaseEntity — DATABASE.md §6.3 does not include
// created_at / updated_at columns for booking_item.
//
// Critical: UNIQUE(event_session_id, seat_id) — the database-level guard against
// selling the same seat twice in the same session. Application checks are
// necessary for clean error messages, but this constraint is the final protection.
//
// BR-09: booking_item.eventSession must always equal booking.eventSession.
// The Service layer must guarantee this invariant when building BookingItems.
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

    // qr_code is generated as a UUID placeholder in US-13.
    // QR content generation/scanning is implemented in a future Ticket US.
    @Column(name = "qr_code", nullable = false, unique = true, length = 255)
    private String qrCode;

    // Final ticket price — immutable once set. Calculated as:
    // eventSession.basePrice × seat.priceMultiplier (HALF_UP, scale 2).
    // Must NOT be accepted from the client (BR-06).
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    // Full enum replaces stub VARCHAR — status values per DATABASE.md §12.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BookingItemStatus status;

    // Immutable snapshot of event/ticket information at booking time.
    // Stored as JSON string so historical records are preserved even if event data changes.
    @Column(name = "event_snapshot", columnDefinition = "TEXT")
    private String eventSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // Must always equal booking.eventSession (BR-09).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;
}
