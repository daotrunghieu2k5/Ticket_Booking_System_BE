package com.dthxhieu.ticket_booking_system_be.booking.controller;

import com.dthxhieu.ticket_booking_system_be.booking.dto.response.PaymentLinkResponse;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.PaymentResponse;
import com.dthxhieu.ticket_booking_system_be.booking.service.PaymentService;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// PaymentController — thin controller for payment endpoints (US-14).
//
// All three endpoints require authentication (covered by anyRequest().authenticated() in SecurityConfig).
// userId is NEVER accepted from the request — resolved from Spring Security principal only.
//
// URL design:
//   POST /api/v1/payments/{bookingId}          — create payment link for a booking
//   GET  /api/v1/payments/{paymentId}          — get payment detail by payment ID
//   GET  /api/v1/bookings/{bookingId}/payment  — get payment detail by booking ID
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    // POST /api/v1/payments/{bookingId}
    // Creates a new PaymentTransaction and returns the PayOS checkout URL.
    // Calling this multiple times is safe — each call creates a new transaction.
    @PostMapping("/api/v1/payments/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentLinkResponse>> createPayment(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        PaymentLinkResponse data = paymentService.createPayment(bookingId, userId);

        return ResponseEntity.ok(ApiResponse.<PaymentLinkResponse>builder()
                .success(true)
                .message("Payment link created successfully.")
                .data(data)
                .build());
    }

    // GET /api/v1/payments/{paymentId}
    // Returns Payment details. Ownership enforced in service layer.
    @GetMapping("/api/v1/payments/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        PaymentResponse data = paymentService.getPaymentById(paymentId, userId);

        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment retrieved successfully.")
                .data(data)
                .build());
    }

    // GET /api/v1/bookings/{bookingId}/payment
    // Returns the Payment linked to a Booking. Ownership enforced in service layer.
    @GetMapping("/api/v1/bookings/{bookingId}/payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByBookingId(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = resolveUserId(userDetails);
        PaymentResponse data = paymentService.getPaymentByBookingId(bookingId, userId);

        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Payment retrieved successfully.")
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
