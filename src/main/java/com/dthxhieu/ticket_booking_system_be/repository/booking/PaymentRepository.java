package com.dthxhieu.ticket_booking_system_be.repository.booking;

import com.dthxhieu.ticket_booking_system_be.entity.booking.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// PaymentRepository.
//
// findByBookingId — used in US-14 to load the Payment when user initiates payment
// (without loading the full Booking entity).
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // US-14: Load Payment by bookingId — safe because payment.booking_id is UNIQUE.
    Optional<Payment> findByBookingId(Long bookingId);
}

