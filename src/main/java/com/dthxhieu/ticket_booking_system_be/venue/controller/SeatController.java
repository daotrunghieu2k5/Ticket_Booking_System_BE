package com.dthxhieu.ticket_booking_system_be.venue.controller;

import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.BatchCreateSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.BatchDeleteSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.CreateSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.UpdateSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.BatchCreateSeatResponse;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.BatchDeleteSeatResponse;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.SeatResponse;
import com.dthxhieu.ticket_booking_system_be.venue.service.SeatService;
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

// URL prefix strategy:
//   /api/v1/venues/{venueId}/seats   — public reads, admin writes nested under venue
//   /api/v1/seats/{id}               — public read by seat id, admin update/delete
//   /api/v1/admin/**                 — protected by SecurityConfig hasRole("ADMIN")
@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    // --- Public Endpoints ---

    @GetMapping("/api/v1/venues/{venueId}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsByVenue(
            @PathVariable Long venueId
    ) {
        List<SeatResponse> data = seatService.getSeatsByVenue(venueId);

        return ResponseEntity.ok(ApiResponse.<List<SeatResponse>>builder()
                .success(true)
                .message("Seats retrieved successfully.")
                .data(data)
                .build());
    }

    @GetMapping("/api/v1/seats/{id}")
    public ResponseEntity<ApiResponse<SeatResponse>> getSeatById(
            @PathVariable Long id
    ) {
        SeatResponse data = seatService.getSeatById(id);

        return ResponseEntity.ok(ApiResponse.<SeatResponse>builder()
                .success(true)
                .message("Seat retrieved successfully.")
                .data(data)
                .build());
    }

    // --- Admin Endpoints ---

    @PostMapping("/api/v1/admin/venues/{venueId}/seats")
    public ResponseEntity<ApiResponse<SeatResponse>> createSeat(
            @PathVariable Long venueId,
            @Valid @RequestBody CreateSeatRequest request
    ) {
        SeatResponse data = seatService.createSeat(venueId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<SeatResponse>builder()
                        .success(true)
                        .message("Seat created successfully.")
                        .data(data)
                        .build());
    }

    @PostMapping("/api/v1/admin/venues/{venueId}/seats/batch")
    public ResponseEntity<ApiResponse<BatchCreateSeatResponse>> batchCreateSeats(
            @PathVariable Long venueId,
            @Valid @RequestBody BatchCreateSeatRequest request
    ) {
        BatchCreateSeatResponse data = seatService.batchCreateSeats(venueId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BatchCreateSeatResponse>builder()
                        .success(true)
                        .message("Seats created successfully.")
                        .data(data)
                        .build());
    }

    @PutMapping("/api/v1/admin/seats/{id}")
    public ResponseEntity<ApiResponse<SeatResponse>> updateSeat(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSeatRequest request
    ) {
        SeatResponse data = seatService.updateSeat(id, request);

        return ResponseEntity.ok(ApiResponse.<SeatResponse>builder()
                .success(true)
                .message("Seat updated successfully.")
                .data(data)
                .build());
    }

    @DeleteMapping("/api/v1/admin/seats/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSeat(
            @PathVariable Long id
    ) {
        seatService.deleteSeat(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Seat deleted successfully.")
                .build());
    }

    @PostMapping("/api/v1/admin/seats/batch-delete")
    public ResponseEntity<ApiResponse<BatchDeleteSeatResponse>> batchDeleteSeats(
            @Valid @RequestBody BatchDeleteSeatRequest request
    ) {
        BatchDeleteSeatResponse data = seatService.batchDeleteSeats(request);

        return ResponseEntity.ok(ApiResponse.<BatchDeleteSeatResponse>builder()
                .success(true)
                .message("Seats deleted successfully.")
                .data(data)
                .build());
    }
}
