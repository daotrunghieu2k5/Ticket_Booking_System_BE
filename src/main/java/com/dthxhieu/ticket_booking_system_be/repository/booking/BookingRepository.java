package com.dthxhieu.ticket_booking_system_be.repository.booking;

import com.dthxhieu.ticket_booking_system_be.entity.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // US-13 FR-14: Get all bookings for the current user (ownership scope).
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // US-13 FR-15/FR-16: Find a booking by ID — ownership check done in service.
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    // US-13 BR-08: Check if a booking code already exists (handle rare collision on generation).
    boolean existsByBookingCode(String bookingCode);
}
