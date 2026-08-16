package com.dthxhieu.ticket_booking_system_be.common.exception;

// Thrown when an authenticated user attempts to access a resource that belongs
// to another user (e.g. viewing another user's Booking, or using another user's SeatHold).
//
// Maps to HTTP 403 FORBIDDEN in GlobalExceptionHandler.
//
// Extends BusinessException so it participates in the same exception hierarchy
// but is caught by a dedicated handler that returns 403 instead of 400.
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(message);
    }
}
