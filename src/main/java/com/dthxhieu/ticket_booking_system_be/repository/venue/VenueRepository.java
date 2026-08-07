package com.dthxhieu.ticket_booking_system_be.repository.venue;

import com.dthxhieu.ticket_booking_system_be.entity.venue.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    // BR-02: Name uniqueness check on create (case-insensitive, ignores trim at service level).
    boolean existsByNameIgnoreCase(String name);

    // BR-02: Name uniqueness check on update, excluding the current record.
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
