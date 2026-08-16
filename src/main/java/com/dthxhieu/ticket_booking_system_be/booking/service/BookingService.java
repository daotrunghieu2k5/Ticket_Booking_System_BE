package com.dthxhieu.ticket_booking_system_be.booking.service;

import com.dthxhieu.ticket_booking_system_be.booking.dto.request.CreateBookingRequest;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.BookingResponse;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.BookingSummaryResponse;

import java.util.List;

public interface BookingService {

    // US-13 FR-01, FR-13: Create a booking from active SeatHolds.
    // Must be transactional — Booking + BookingItems + Payment in one unit.
    // userId comes from the authenticated security context, never from the request.
    BookingResponse createBooking(CreateBookingRequest request, Long userId);

    // US-13 FR-14: Retrieve the current user's own bookings (summary list).
    List<BookingSummaryResponse> getMyBookings(Long userId);

    // US-13 FR-15, FR-16: Retrieve full detail for a single booking.
    // Throws ForbiddenException if the booking belongs to a different user.
    BookingResponse getBookingDetail(Long bookingId, Long userId);
}
