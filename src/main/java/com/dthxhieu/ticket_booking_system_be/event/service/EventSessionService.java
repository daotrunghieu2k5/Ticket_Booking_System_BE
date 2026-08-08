package com.dthxhieu.ticket_booking_system_be.event.service;

import com.dthxhieu.ticket_booking_system_be.common.enums.SessionStatus;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.CreateEventSessionRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.UpdateEventSessionRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventSessionResponse;

import java.util.List;

public interface EventSessionService {

    // Returns all event sessions, filtered by optional eventId/venueId/status (FR-01).
    List<EventSessionResponse> getEventSessions(Long eventId, Long venueId, SessionStatus status);

    // Returns a single event session by ID (FR-02).
    EventSessionResponse getEventSessionById(Long id);

    // Creates a new event session — admin only (FR-03).
    EventSessionResponse createEventSession(CreateEventSessionRequest request);

    // Updates an existing event session — admin only (FR-04).
    EventSessionResponse updateEventSession(Long id, UpdateEventSessionRequest request);

    // Cancels a session by setting status = CANCELLED — admin only (FR-05, BR-10).
    void cancelEventSession(Long id);
}
