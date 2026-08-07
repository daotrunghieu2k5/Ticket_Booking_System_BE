package com.dthxhieu.ticket_booking_system_be.repository.venue;

import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // US-08: Count seats for a venue — used in VenueServiceImpl delete-safety (BR-07).
    long countByVenueId(Long venueId);

    // US-09: Retrieve all seats of a venue ordered by row then seat number for consistent display.
    List<Seat> findByVenueIdOrderByRowNameAscSeatNumberAsc(Long venueId);

    // US-09: Duplicate position check on create (BR-04).
    // Case-insensitive on rowName so "a" and "A" are treated as the same row.
    boolean existsByVenueIdAndRowNameIgnoreCaseAndSeatNumber(Long venueId, String rowName, Integer seatNumber);

    // US-09: Duplicate position check on update, excluding the seat being updated (BR-06).
    boolean existsByVenueIdAndRowNameIgnoreCaseAndSeatNumberAndIdNot(Long venueId, String rowName, Integer seatNumber, Long id);
}
