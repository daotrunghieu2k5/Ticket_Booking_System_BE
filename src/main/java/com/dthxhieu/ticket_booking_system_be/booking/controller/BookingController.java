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

// Thin BookingController for US-13.
//
// All endpoints require authentication — handled by anyRequest().authenticated() in SecurityConfig.
// userId is NEVER accepted from the request — always resolved from Spring Security context.
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    // FR-01: Create booking from held seats. Authenticated only.
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

    // FR-14: Get current user's bookings. Authenticated only.
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

    // FR-15/FR-16: Get booking detail — ownership enforced in service.
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        BookingResponse data = bookingService.getBookingById(bookingId, userId);

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Booking retrieved successfully.")
                .data(data)
                .build());
    }

    // Resolves the current authenticated user's ID from Spring Security context.
    // Never trusts userId from request body or URL — JWT/session principal is the source of truth.
    private Long resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return user.getId();
    }
}
