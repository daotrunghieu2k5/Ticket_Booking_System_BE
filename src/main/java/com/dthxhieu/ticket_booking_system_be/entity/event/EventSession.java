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

// Stub EventSession entity for US-08 scope.
// Full event session management will be implemented in a later user story.
// This stub exists so EventSessionRepository.existsByVenueId() can be used
// for the delete-safety check in VenueServiceImpl (BR-06).
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

    @Column(name = "booking_open", nullable = false)
    private LocalDateTime bookingOpen;

    @Column(name = "booking_close", nullable = false)
    private LocalDateTime bookingClose;

    // base_price stored as DECIMAL(15,2) to represent currency without floating-point errors.
    @Column(name = "base_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SessionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // venue_id FK enables EventSessionRepository.existsByVenueId(Long) for delete-safety.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;
}
