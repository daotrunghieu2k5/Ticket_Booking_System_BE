package com.dthxhieu.ticket_booking_system_be.booking.controller;

import com.dthxhieu.ticket_booking_system_be.booking.dto.request.HoldSeatsRequest;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.SeatAvailabilityItem;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.SeatHoldResponse;
import com.dthxhieu.ticket_booking_system_be.booking.service.SeatHoldService;
import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Thin controller for Seat Availability and Seat Hold endpoints (US-12).
//
// URL design:
//   GET    /api/v1/event-sessions/{sessionId}/seats           — public (seat map)
//   POST   /api/v1/event-sessions/{sessionId}/seat-holds      — authenticated
//   DELETE /api/v1/seat-holds/{holdId}                        — authenticated
//   GET    /api/v1/event-sessions/{sessionId}/seat-holds/me   — authenticated
//
// Current user is resolved from Spring Security context via @AuthenticationPrincipal.
// userId is NEVER accepted from the request body or URL — only from the security context.
@RestController
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldService seatHoldService;
    private final UserRepository userRepository;

    // --- Public Endpoint ---

    // FR-01: Seat map is public — no authentication required.
    // The /api/v1/event-sessions/** permitAll() rule in SecurityConfig covers this.
    @GetMapping("/api/v1/event-sessions/{sessionId}/seats")
    public ResponseEntity<ApiResponse<List<SeatAvailabilityItem>>> getSeatMap(
            @PathVariable Long sessionId
    ) {
        List<SeatAvailabilityItem> data = seatHoldService.getSeatMap(sessionId);

        return ResponseEntity.ok(ApiResponse.<List<SeatAvailabilityItem>>builder()
                .success(true)
                .message("Seats retrieved successfully.")
                .data(data)
                .build());
    }

    // --- Authenticated Endpoints ---

    // FR-07: Customer holds seats. Requires authentication.
    // Falls under anyRequest().authenticated() in SecurityConfig.
    @PostMapping("/api/v1/event-sessions/{sessionId}/seat-holds")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> holdSeats(
            @PathVariable Long sessionId,
            @Valid @RequestBody HoldSeatsRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        SeatHoldResponse data = seatHoldService.holdSeats(sessionId, request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<SeatHoldResponse>builder()
                        .success(true)
                        .message("Seats held successfully.")
                        .data(data)
                        .build());
    }

    // FR-14: Customer releases their own hold. Ownership verified in service.
    @DeleteMapping("/api/v1/seat-holds/{holdId}")
    public ResponseEntity<ApiResponse<Void>> releaseHold(
            @PathVariable Long holdId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        seatHoldService.releaseHold(holdId, userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Seat hold released successfully.")
                .build());
    }

    // §6.4: Retrieve the current user's active hold for a specific session.
    @GetMapping("/api/v1/event-sessions/{sessionId}/seat-holds/me")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> getMyActiveHold(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        SeatHoldResponse data = seatHoldService.getMyActiveHold(sessionId, userId);

        return ResponseEntity.ok(ApiResponse.<SeatHoldResponse>builder()
                .success(true)
                .message("Active seat hold retrieved successfully.")
                .data(data)
                .build());
    }

    // --- Private Helper ---

    // Resolves the authenticated user's ID from Spring Security context.
    // Never trusts userId from request body/URL — always from JWT/session principal.
    private Long resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException("User not found."));
        return user.getId();
    }
}
