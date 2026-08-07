package com.dthxhieu.ticket_booking_system_be.repository.event;

import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSessionRepository extends JpaRepository<EventSession, Long> {

    // US-08: Check if any event session references this venue before deletion.
    boolean existsByVenueId(Long venueId);

    // US-10: Check if any event session belongs to an event before deletion.
    // If true, the event must be soft-deleted (INACTIVE) instead of physically deleted (BR-13).
    boolean existsByEventId(Long eventId);
}
