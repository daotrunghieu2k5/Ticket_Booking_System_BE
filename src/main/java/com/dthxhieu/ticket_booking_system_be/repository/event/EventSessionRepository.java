package com.dthxhieu.ticket_booking_system_be.repository.event;

import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSessionRepository extends JpaRepository<EventSession, Long> {

    // BR-06: Check if any event session references this venue before deletion.
    boolean existsByVenueId(Long venueId);
}
