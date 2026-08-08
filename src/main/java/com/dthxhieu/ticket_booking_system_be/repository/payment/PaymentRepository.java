package com.dthxhieu.ticket_booking_system_be.repository.payment;

import com.dthxhieu.ticket_booking_system_be.entity.booking.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // PaymentRepository created for US-13.
    // US-13 creates payments via CascadeType.ALL on Booking → Payment.
    // Direct PaymentRepository access may be needed for future Payment US.
}
