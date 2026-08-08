package com.dthxhieu.ticket_booking_system_be.event.service.impl;

import com.dthxhieu.ticket_booking_system_be.common.enums.SessionStatus;
import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.event.Event;
import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Venue;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.CreateEventSessionRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.UpdateEventSessionRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventSessionResponse;
import com.dthxhieu.ticket_booking_system_be.event.mapper.EventSessionMapper;
import com.dthxhieu.ticket_booking_system_be.event.service.EventSessionService;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventRepository;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventSessionRepository;
import com.dthxhieu.ticket_booking_system_be.repository.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventSessionServiceImpl implements EventSessionService {

    private final EventSessionRepository eventSessionRepository;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final EventSessionMapper eventSessionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EventSessionResponse> getEventSessions(
            Long eventId,
            Long venueId,
            SessionStatus status
    ) {
        return eventSessionRepository.findByFilters(eventId, venueId, status)
                .stream()
                .map(eventSessionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventSessionResponse getEventSessionById(Long id) {
        EventSession session = eventSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event session not found."));

        return eventSessionMapper.toResponse(session);
    }

    @Override
    public EventSessionResponse createEventSession(CreateEventSessionRequest request) {
        // FR-08: Validate Event exists.
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));

        // FR-09: Validate Venue exists.
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found."));

        // Run all business rule validations before persisting.
        validateSchedule(request.getStartTime(), request.getEndTime(),
                request.getBookingOpen(), request.getBookingClose());

        // BR-08: Reject conflicting sessions at the same Venue.
        if (eventSessionRepository.existsOverlappingSession(
                venue.getId(), request.getStartTime(), request.getEndTime())) {
            throw new BusinessException(
                    "An event session already exists at this venue during the requested time slot."
            );
        }

        // BR-04: EventSession creation must NOT create any Seat records.
        // Seats belong to the Venue and are reused automatically by this session.

        EventSession session = EventSession.builder()
                .event(event)
                .venue(venue)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .bookingOpen(request.getBookingOpen())
                .bookingClose(request.getBookingClose())
                .basePrice(request.getBasePrice())
                // BR-09: Default status on creation is UPCOMING.
                .status(SessionStatus.UPCOMING)
                .build();

        return eventSessionMapper.toResponse(eventSessionRepository.save(session));
    }

    @Override
    public EventSessionResponse updateEventSession(Long id, UpdateEventSessionRequest request) {
        EventSession session = eventSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event session not found."));

        // FR-08: Validate new Event reference.
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found."));

        // FR-09: Validate new Venue reference.
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found."));

        validateSchedule(request.getStartTime(), request.getEndTime(),
                request.getBookingOpen(), request.getBookingClose());

        // BR-08: Overlap check excludes the current session (self-conflict prevention).
        if (eventSessionRepository.existsOverlappingSessionExcluding(
                venue.getId(), request.getStartTime(), request.getEndTime(), id)) {
            throw new BusinessException(
                    "An event session already exists at this venue during the requested time slot."
            );
        }

        session.setEvent(event);
        session.setVenue(venue);
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setBookingOpen(request.getBookingOpen());
        session.setBookingClose(request.getBookingClose());
        session.setBasePrice(request.getBasePrice());

        return eventSessionMapper.toResponse(eventSessionRepository.save(session));
    }

    @Override
    public void cancelEventSession(Long id) {
        EventSession session = eventSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event session not found."));

        // Spec §12: Cannot cancel an already cancelled session.
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessException("Event session is already cancelled.");
        }

        // Spec §12: Do not cancel a finished session.
        if (session.getStatus() == SessionStatus.FINISHED) {
            throw new BusinessException("A finished event session cannot be cancelled.");
        }

        // BR-10: Status update to CANCELLED — NOT a physical delete.
        // BR-11: Existing Booking and BookingItem records are preserved (only status changes).
        session.setStatus(SessionStatus.CANCELLED);
        eventSessionRepository.save(session);
    }

    // --- Private Helpers ---

    // Validates all time-ordering business rules for create and update.
    // Cross-field validation belongs in Service per spec §7.
    private void validateSchedule(
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime bookingOpen,
            LocalDateTime bookingClose
    ) {
        // BR-05: start_time < end_time.
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("Start time must be before end time.");
        }

        // BR-06: booking_open < booking_close.
        if (!bookingOpen.isBefore(bookingClose)) {
            throw new BusinessException("Booking open time must be before booking close time.");
        }

        // BR-06: booking_close <= start_time (bookings must close before the session starts).
        if (bookingClose.isAfter(startTime)) {
            throw new BusinessException("Booking close time must not be after the session start time.");
        }
    }
}
