package com.dthxhieu.ticket_booking_system_be.repository.booking;

import com.dthxhieu.ticket_booking_system_be.entity.booking.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    // BR-07: Check if a seat is referenced by any booking item before deletion.
    // Prevents deleting seats that are part of booking history.
    boolean existsBySeatId(Long seatId);
}
