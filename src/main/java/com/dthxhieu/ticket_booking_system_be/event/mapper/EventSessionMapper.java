package com.dthxhieu.ticket_booking_system_be.event.mapper;

import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventSessionResponse;
import org.springframework.stereotype.Component;

@Component
public class EventSessionMapper {

    // Maps EventSession entity to EventSessionResponse DTO.
    // eventId and venueId are extracted as flat IDs per the API contract (§6.1, §6.2).
    // No nested objects are needed — clients resolve event/venue details via their own endpoints.
    public EventSessionResponse toResponse(EventSession session) {
        return EventSessionResponse.builder()
                .id(session.getId())
                .eventId(session.getEvent().getId())
                .venueId(session.getVenue().getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .bookingOpen(session.getBookingOpen())
                .bookingClose(session.getBookingClose())
                .basePrice(session.getBasePrice())
                .status(session.getStatus())
                .build();
    }
}
