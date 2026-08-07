package com.dthxhieu.ticket_booking_system_be.entity.booking;

import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// Stub BookingItem entity for US-09 scope.
// Full booking item management will be implemented in the Booking module.
// This stub exists so BookingItemRepository.existsBySeatId() can be used
// for the delete-safety check in SeatServiceImpl (BR-07).
//
// BookingItem does NOT extend BaseEntity — DATABASE.md §6.3 does not list
// created_at / updated_at columns for booking_item.
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

    @Column(name = "qr_code", nullable = false, unique = true, length = 255)
    private String qrCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    // Status stored as VARCHAR — full enum defined in Booking module US.
    @Column(nullable = false, length = 50)
    private String status;

    // Immutable snapshot of ticket information stored as text/JSON.
    @Column(name = "event_snapshot", columnDefinition = "TEXT")
    private String eventSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // seat_id FK enables BookingItemRepository.existsBySeatId() for delete-safety.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;
}
