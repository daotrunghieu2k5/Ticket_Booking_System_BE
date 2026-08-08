package com.dthxhieu.ticket_booking_system_be.entity.event;

import com.dthxhieu.ticket_booking_system_be.common.enums.SessionStatus;
import com.dthxhieu.ticket_booking_system_be.entity.BaseEntity;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Venue;
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
import java.time.LocalDateTime;

// EventSession represents one scheduled showing of an Event at a specific Venue (US-11).
//
// Key architectural rules (US-11 §1, BR-03, BR-04):
//   - Seats belong to Venue, NOT to EventSession.
//   - Creating an EventSession MUST NOT create new Seat records.
//   - Seat availability is determined per-session via SeatHold and BookingItem.
//
// Unique constraint (event_id, venue_id, start_time) prevents scheduling the same
// event at the same venue at the exact same start time.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "event_session",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "venue_id", "start_time"})
)
public class EventSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // bookingOpen: when ticket purchasing becomes available.
    @Column(name = "booking_open", nullable = false)
    private LocalDateTime bookingOpen;

    // bookingClose: must be <= startTime (BR-06) — no booking after session starts.
    @Column(name = "booking_close", nullable = false)
    private LocalDateTime bookingClose;

    // Base ticket price. Final price = basePrice × seat.priceMultiplier.
    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    // Session lifecycle status. Default on create: UPCOMING.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SessionStatus status;

    // FK to Event. Many sessions can belong to one event (e.g., multiple showings).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // FK to Venue. Venue's physical seats are reused by this session (BR-03).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;
}
