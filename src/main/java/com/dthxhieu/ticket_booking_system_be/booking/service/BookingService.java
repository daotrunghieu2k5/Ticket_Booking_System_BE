package com.dthxhieu.ticket_booking_system_be.booking.service;

import com.dthxhieu.ticket_booking_system_be.booking.dto.request.CreateBookingRequest;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.BookingResponse;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.BookingSummaryResponse;

import java.util.List;

public interface BookingService {

    // FR-01: Create a booking from held seats (authenticated, all-or-nothing).
    BookingResponse createBooking(CreateBookingRequest request, Long userId);

    // FR-14: Get current user's booking list (summary only — no N+1).
    List<BookingSummaryResponse> getMyBookings(Long userId);

    // FR-15/FR-16: Get booking detail — ownership validated in service.
    BookingResponse getBookingById(Long bookingId, Long userId);
}
