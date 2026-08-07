package com.dthxhieu.ticket_booking_system_be.venue.service;

import com.dthxhieu.ticket_booking_system_be.venue.dto.request.CreateVenueRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.UpdateVenueRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.VenueResponse;

import java.util.List;

public interface VenueService {

    // Returns all venues — available to public users (FR-01).
    List<VenueResponse> getAllVenues();

    // Returns one venue by id — available to public users (FR-02).
    VenueResponse getVenueById(Long id);

    // Creates a new venue — admin only (FR-03, BR-08).
    VenueResponse createVenue(CreateVenueRequest request);

    // Updates an existing venue — admin only (FR-04, BR-08).
    VenueResponse updateVenue(Long id, UpdateVenueRequest request);

    // Deletes a venue if it is not referenced by any event session or seat — admin only (FR-05, BR-06, BR-07, BR-08).
    void deleteVenue(Long id);
}
