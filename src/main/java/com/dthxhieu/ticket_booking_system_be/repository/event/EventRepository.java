package com.dthxhieu.ticket_booking_system_be.repository.event;

import com.dthxhieu.ticket_booking_system_be.entity.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByCategoryId(Long categoryId);
}
