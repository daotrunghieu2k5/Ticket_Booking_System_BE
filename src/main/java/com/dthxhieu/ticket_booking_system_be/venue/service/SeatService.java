package com.dthxhieu.ticket_booking_system_be.venue.service;

import com.dthxhieu.ticket_booking_system_be.venue.dto.request.BatchCreateSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.BatchDeleteSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.CreateSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.UpdateSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.BatchCreateSeatResponse;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.BatchDeleteSeatResponse;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.SeatResponse;

import java.util.List;

public interface SeatService {

    // Returns all seats for a given venue, ordered by row then seat number (FR-01).
    List<SeatResponse> getSeatsByVenue(Long venueId);

    // Returns a single seat by id (FR-02).
    SeatResponse getSeatById(Long id);

    // Creates a single seat in a venue — admin only (FR-03, BR-12).
    SeatResponse createSeat(Long venueId, CreateSeatRequest request);

    // Generates and creates a range of seats in batch — admin only (FR-04, BR-12).
    BatchCreateSeatResponse batchCreateSeats(Long venueId, BatchCreateSeatRequest request);

    // Updates an existing seat — admin only (FR-05, BR-12).
    SeatResponse updateSeat(Long id, UpdateSeatRequest request);

    // Deletes a single seat if not referenced by any booking — admin only (FR-06, BR-07, BR-12).
    void deleteSeat(Long id);

    // Atomically deletes multiple seats — admin only (FR-07, BR-11, BR-12).
    BatchDeleteSeatResponse batchDeleteSeats(BatchDeleteSeatRequest request);
}
