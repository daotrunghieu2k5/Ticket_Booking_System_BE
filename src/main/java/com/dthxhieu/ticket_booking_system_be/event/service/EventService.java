package com.dthxhieu.ticket_booking_system_be.event.service;

import com.dthxhieu.ticket_booking_system_be.common.enums.EventStatus;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.CreateEventRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.UpdateEventRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventDetailResponse;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventPageResponse;
import org.springframework.data.domain.Pageable;

public interface EventService {

    // Returns a paginated, filtered list of events (FR-01, FR-03, FR-04, FR-05).
    // Public callers receive only ACTIVE events (BR-11); status param is admin-only.
    EventPageResponse getEvents(String keyword, Long categoryId, EventStatus status, Pageable pageable);

    // Returns full event detail by id (FR-02).
    EventDetailResponse getEventById(Long id);

    // Creates a new event — admin only (FR-06, BR-12).
    EventDetailResponse createEvent(CreateEventRequest request);

    // Updates an existing event — admin only (FR-07, BR-12).
    EventDetailResponse updateEvent(Long id, UpdateEventRequest request);

    // Deletes or soft-deactivates an event — admin only (FR-08, FR-09, BR-13, BR-14).
    void deleteEvent(Long id);

    // Sets event status to ACTIVE (FR-11, BR-12).
    void activateEvent(Long id);

    // Sets event status to INACTIVE (FR-11, BR-12).
    void deactivateEvent(Long id);
}
