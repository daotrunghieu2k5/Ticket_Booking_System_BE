package com.dthxhieu.ticket_booking_system_be.entity.booking;

import com.dthxhieu.ticket_booking_system_be.common.enums.SeatHoldStatus;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// SeatHold temporarily reserves a physical Seat for a specific EventSession (US-12).
//
// Does NOT extend BaseEntity because DATABASE.md §6.1 does not include updated_at
// for seat_hold. Extending BaseEntity would inject an unmapped updated_at column.
//
// Concurrency protection: a partial unique index (WHERE status = 'ACTIVE') in the
// database enforces at most one ACTIVE hold per (event_session_id, seat_id).
// EXPIRED and CANCELLED rows are not covered by the index → they do not block re-holding.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seat_hold")
@EntityListeners(AuditingEntityListener.class)
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Opaque identifier for this hold (UUID). Stored for future lookup/idempotency.
    // Not exposed in the current API response — clients use holdId (the PK).
    @Column(name = "hold_token", nullable = false, length = 36)
    private String holdToken;

    // Hold lifecycle status. ACTIVE is the only status covered by the partial unique index.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatHoldStatus status;

    // Timestamp when this hold was created (set automatically by JPA auditing).
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Timestamp after which this hold is logically expired.
    // Active holds: status = ACTIVE AND expired_at > now.
    // Logically expired: status = ACTIVE AND expired_at <= now.
    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    // The user who created this hold. Ownership is verified on release (BR-05).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The EventSession this hold is for. Seat availability is session-specific.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;

    // The physical Seat being held. Seat belongs to Venue, not to the session.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;
}
