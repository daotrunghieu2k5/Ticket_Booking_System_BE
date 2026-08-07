package com.dthxhieu.ticket_booking_system_be.venue.controller;

import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.CreateVenueRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.UpdateVenueRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.VenueResponse;
import com.dthxhieu.ticket_booking_system_be.venue.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Two URL prefixes are used intentionally:
//   /api/v1/venues        — public read endpoints (FR-01, FR-02)
//   /api/v1/admin/venues  — admin write endpoints (FR-03, FR-04, FR-05, BR-08)
// SecurityConfig protects /api/v1/admin/** with hasRole("ADMIN") in a single rule.
@RestController
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    // --- Public Endpoints ---

    @GetMapping("/api/v1/venues")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getAllVenues() {
        List<VenueResponse> data = venueService.getAllVenues();

        return ResponseEntity.ok(ApiResponse.<List<VenueResponse>>builder()
                .success(true)
                .message("Venues retrieved successfully.")
                .data(data)
                .build());
    }

    @GetMapping("/api/v1/venues/{id}")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueById(
            @PathVariable Long id
    ) {
        VenueResponse data = venueService.getVenueById(id);

        return ResponseEntity.ok(ApiResponse.<VenueResponse>builder()
                .success(true)
                .message("Venue retrieved successfully.")
                .data(data)
                .build());
    }

    // --- Admin Endpoints ---

    @PostMapping("/api/v1/admin/venues")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(
            @Valid @RequestBody CreateVenueRequest request
    ) {
        VenueResponse data = venueService.createVenue(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<VenueResponse>builder()
                        .success(true)
                        .message("Venue created successfully.")
                        .data(data)
                        .build());
    }

    @PutMapping("/api/v1/admin/venues/{id}")
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenue(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVenueRequest request
    ) {
        VenueResponse data = venueService.updateVenue(id, request);

        return ResponseEntity.ok(ApiResponse.<VenueResponse>builder()
                .success(true)
                .message("Venue updated successfully.")
                .data(data)
                .build());
    }

    @DeleteMapping("/api/v1/admin/venues/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVenue(
            @PathVariable Long id
    ) {
        venueService.deleteVenue(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Venue deleted successfully.")
                .build());
    }
}
