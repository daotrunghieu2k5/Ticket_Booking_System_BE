package com.dthxhieu.ticket_booking_system_be.event.controller;

import com.dthxhieu.ticket_booking_system_be.common.enums.SessionStatus;
import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.CreateEventSessionRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.UpdateEventSessionRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventSessionResponse;
import com.dthxhieu.ticket_booking_system_be.event.service.EventSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// URL prefix strategy:
//   /api/v1/event-sessions       — public read endpoints (FR-01, FR-02)
//   /api/v1/admin/event-sessions — admin write endpoints (FR-03, FR-04, FR-05)
// SecurityConfig protects /api/v1/admin/** with hasRole("ADMIN") globally.
@RestController
@RequiredArgsConstructor
public class EventSessionController {

    private final EventSessionService eventSessionService;

    // --- Public Endpoints ---

    @GetMapping("/api/v1/event-sessions")
    public ResponseEntity<ApiResponse<List<EventSessionResponse>>> getEventSessions(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) SessionStatus status
    ) {
        List<EventSessionResponse> data = eventSessionService.getEventSessions(eventId, venueId, status);

        return ResponseEntity.ok(ApiResponse.<List<EventSessionResponse>>builder()
                .success(true)
                .message("Event sessions retrieved successfully.")
                .data(data)
                .build());
    }

    @GetMapping("/api/v1/event-sessions/{id}")
    public ResponseEntity<ApiResponse<EventSessionResponse>> getEventSessionById(
            @PathVariable Long id
    ) {
        EventSessionResponse data = eventSessionService.getEventSessionById(id);

        return ResponseEntity.ok(ApiResponse.<EventSessionResponse>builder()
                .success(true)
                .message("Event session retrieved successfully.")
                .data(data)
                .build());
    }

    // --- Admin Endpoints ---

    @PostMapping("/api/v1/admin/event-sessions")
    public ResponseEntity<ApiResponse<EventSessionResponse>> createEventSession(
            @Valid @RequestBody CreateEventSessionRequest request
    ) {
        EventSessionResponse data = eventSessionService.createEventSession(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<EventSessionResponse>builder()
                        .success(true)
                        .message("Event session created successfully.")
                        .data(data)
                        .build());
    }

    @PutMapping("/api/v1/admin/event-sessions/{id}")
    public ResponseEntity<ApiResponse<EventSessionResponse>> updateEventSession(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventSessionRequest request
    ) {
        EventSessionResponse data = eventSessionService.updateEventSession(id, request);

        return ResponseEntity.ok(ApiResponse.<EventSessionResponse>builder()
                .success(true)
                .message("Event session updated successfully.")
                .data(data)
                .build());
    }

    @PatchMapping("/api/v1/admin/event-sessions/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelEventSession(
            @PathVariable Long id
    ) {
        eventSessionService.cancelEventSession(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Event session cancelled successfully.")
                .build());
    }
}
