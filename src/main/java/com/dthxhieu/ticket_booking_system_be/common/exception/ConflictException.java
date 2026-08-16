package com.dthxhieu.ticket_booking_system_be.common.exception;

// Thrown when a business operation conflicts with the current resource state.
// Examples:
//   - SeatHold has expired.
//   - Seat has already been booked.
//   - EventSession is not accepting bookings.
//   - Concurrent booking request caused a DB unique constraint violation.
//
// Maps to HTTP 409 CONFLICT in GlobalExceptionHandler.
//
// Extends BusinessException to stay in the same exception hierarchy.
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message);
    }
}
