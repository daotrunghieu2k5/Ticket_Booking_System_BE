package com.dthxhieu.ticket_booking_system_be.booking.controller;

import com.dthxhieu.ticket_booking_system_be.booking.dto.request.CreateBookingRequest;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.BookingResponse;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.BookingSummaryResponse;
import com.dthxhieu.ticket_booking_system_be.booking.service.BookingService;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Thin controller for Booking endpoints (US-13).
//
// All three endpoints require authentication — covered by anyRequest().authenticated()
// in SecurityConfig. No public endpoints in this controller.
//
// Current user is resolved via @AuthenticationPrincipal → email → UserRepository.
// userId is NEVER accepted from the request body or URL (BR-14).
//
// URL design:
//   POST /api/v1/bookings       — create booking from active SeatHolds
//   GET  /api/v1/bookings/me    — retrieve current user's bookings
//   GET  /api/v1/bookings/{id}  — retrieve booking detail (ownership enforced)
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    // POST /api/v1/bookings
    // FR-01: Authenticated user creates a booking from their active SeatHolds.
    // Returns 201 CREATED with full BookingResponse.
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        BookingResponse data = bookingService.createBooking(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BookingResponse>builder()
                        .success(true)
                        .message("Booking created successfully.")
                        .data(data)
                        .build());
    }

    // GET /api/v1/bookings/me
    // FR-14: Retrieve all bookings belonging to the current user.
    // Must be declared before /{id} to avoid Spring treating "me" as a path variable.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<BookingSummaryResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        List<BookingSummaryResponse> data = bookingService.getMyBookings(userId);

        return ResponseEntity.ok(ApiResponse.<List<BookingSummaryResponse>>builder()
                .success(true)
                .message("Bookings retrieved successfully.")
                .data(data)
                .build());
    }

    // GET /api/v1/bookings/{id}
    // FR-15, FR-16: Retrieve a specific booking.
    // Service enforces ownership — returns 403 if booking belongs to another user.
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingDetail(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        BookingResponse data = bookingService.getBookingDetail(bookingId, userId);

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Booking retrieved successfully.")
                .data(data)
                .build());
    }

    // Resolves the authenticated user's ID from the Spring Security principal.
    // Email is the principal name (set during JWT parsing in the security filter).
    // userId is never trusted from the request — only from this security context.
    private Long resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return user.getId();
    }
}
