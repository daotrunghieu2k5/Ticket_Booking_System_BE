package com.dthxhieu.ticket_booking_system_be.entity.venue;

import com.dthxhieu.ticket_booking_system_be.common.enums.SeatType;
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

// Seat does NOT extend BaseEntity — DATABASE.md §5.3 does not include
// created_at / updated_at audit columns for seats (physical, permanent entities).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "seat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"venue_id", "row_name", "seat_number"})
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optional section label (e.g. "Zone A"). Not sent in create/update requests.
    @Column(length = 100)
    private String section;

    // Row identifier (e.g. "A", "B", "AA"). Always stored in uppercase.
    @Column(name = "row_name", nullable = false, length = 10)
    private String rowName;

    // Seat number within the row. Must be > 0 (BR-03).
    // Column type is INTEGER after V13 migration.
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    // Seat classification stored as VARCHAR via @Enumerated(STRING).
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 50)
    private SeatType seatType;

    // Pricing multiplier applied to base_price at booking time.
    // Set automatically by the service based on SeatType — not sent in the request.
    @Column(name = "price_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal priceMultiplier;

    // Whether the seat is available for booking. Defaults to true on creation.
    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;
}
