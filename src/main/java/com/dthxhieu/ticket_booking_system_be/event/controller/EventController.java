package com.dthxhieu.ticket_booking_system_be.event.controller;

import com.dthxhieu.ticket_booking_system_be.common.enums.EventStatus;
import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.CreateEventRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.request.UpdateEventRequest;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventDetailResponse;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventPageResponse;
import com.dthxhieu.ticket_booking_system_be.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// URL prefix strategy:
//   /api/v1/events        — public read endpoints (FR-01, FR-02, FR-03, FR-04)
//   /api/v1/admin/events  — admin write endpoints (FR-06, FR-07, FR-08, FR-11)
// SecurityConfig protects /api/v1/admin/** with hasRole("ADMIN") in a single rule.
@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // --- Public Endpoints ---

    // BR-11: Public users see only ACTIVE events — status fixed to ACTIVE here.
    // Admin users access all events via an admin-specific endpoint if needed in the future.
    @GetMapping("/api/v1/events")
    public ResponseEntity<ApiResponse<EventPageResponse>> getEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort
    ) {
        // Restrict public access to ACTIVE events only (BR-11).
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort));
        EventPageResponse data = eventService.getEvents(keyword, categoryId, EventStatus.ACTIVE, pageable);

        return ResponseEntity.ok(ApiResponse.<EventPageResponse>builder()
                .success(true)
                .message("Events retrieved successfully.")
                .data(data)
                .build());
    }

    @GetMapping("/api/v1/events/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEventById(
            @PathVariable Long id
    ) {
        EventDetailResponse data = eventService.getEventById(id);

        return ResponseEntity.ok(ApiResponse.<EventDetailResponse>builder()
                .success(true)
                .message("Event retrieved successfully.")
                .data(data)
                .build());
    }

    // --- Admin Endpoints ---

    @PostMapping("/api/v1/admin/events")
    public ResponseEntity<ApiResponse<EventDetailResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request
    ) {
        EventDetailResponse data = eventService.createEvent(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<EventDetailResponse>builder()
                        .success(true)
                        .message("Event created successfully.")
                        .data(data)
                        .build());
    }

    @PutMapping("/api/v1/admin/events/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        EventDetailResponse data = eventService.updateEvent(id, request);

        return ResponseEntity.ok(ApiResponse.<EventDetailResponse>builder()
                .success(true)
                .message("Event updated successfully.")
                .data(data)
                .build());
    }

    @DeleteMapping("/api/v1/admin/events/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long id
    ) {
        eventService.deleteEvent(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Event deleted successfully.")
                .build());
    }

    @PatchMapping("/api/v1/admin/events/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateEvent(
            @PathVariable Long id
    ) {
        eventService.activateEvent(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Event activated successfully.")
                .build());
    }

    @PatchMapping("/api/v1/admin/events/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateEvent(
            @PathVariable Long id
    ) {
        eventService.deactivateEvent(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Event deactivated successfully.")
                .build());
    }
}
