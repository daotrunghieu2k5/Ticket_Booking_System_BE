package com.dthxhieu.ticket_booking_system_be.repository.booking;

import com.dthxhieu.ticket_booking_system_be.entity.booking.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    // US-09: Check if a seat is referenced by any booking item before deletion.
    // Prevents deleting seats that are part of booking history.
    boolean existsBySeatId(Long seatId);

    // US-12: Seat map batch query — returns all booked seat IDs for a specific EventSession.
    // Using projection (seat.id only) avoids loading full BookingItem entities.
    // This is one of three batch queries used to build the seat map in O(1) per seat.
    @Query("SELECT b.seat.id FROM BookingItem b WHERE b.eventSession.id = :sessionId")
    List<Long> findBookedSeatIdsByEventSessionId(@Param("sessionId") Long sessionId);
}
