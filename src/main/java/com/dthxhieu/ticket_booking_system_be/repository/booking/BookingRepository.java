package com.dthxhieu.ticket_booking_system_be.repository.booking;

import com.dthxhieu.ticket_booking_system_be.entity.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // US-13: Retrieve all bookings for a specific user, newest first.
    // Used by GET /api/v1/bookings/me.
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    // US-13: Ownership-safe booking lookup.
    // Returns empty if the booking exists but belongs to a different user.
    // Used by GET /api/v1/bookings/{id} to enforce BR-14.
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    // US-13: Uniqueness check for booking codes before persisting.
    // The DB constraint is the final protection; this check provides a clean error message.
    boolean existsByBookingCode(String bookingCode);
}
