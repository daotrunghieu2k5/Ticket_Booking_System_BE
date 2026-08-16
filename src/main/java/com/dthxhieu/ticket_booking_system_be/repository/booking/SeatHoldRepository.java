package com.dthxhieu.ticket_booking_system_be.repository.booking;

import com.dthxhieu.ticket_booking_system_be.entity.booking.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    // US-12 Seat Map — batch query.
    // Returns only seat IDs (not full entities) to avoid loading SeatHold data we don't need.
    // Active = status ACTIVE AND expired_at > now (logically expired rows are excluded).
    @Query("""
            SELECT s.seat.id FROM SeatHold s
            WHERE s.eventSession.id = :sessionId
              AND s.status = com.dthxhieu.ticket_booking_system_be.common.enums.SeatHoldStatus.ACTIVE
              AND s.expiredAt > :now
            """)
    List<Long> findActiveHeldSeatIds(
            @Param("sessionId") Long sessionId,
            @Param("now") LocalDateTime now
    );

    // US-12 Hold Seats — find logically expired ACTIVE rows for specific seats.
    // These rows are still status=ACTIVE but expired_at has passed.
    // They must be cleaned up (set to EXPIRED or deleted) before inserting a new hold,
    // otherwise they continue to occupy the partial unique index slot.
    @Query("""
            SELECT s FROM SeatHold s
            WHERE s.eventSession.id = :sessionId
              AND s.seat.id IN :seatIds
              AND s.status = com.dthxhieu.ticket_booking_system_be.common.enums.SeatHoldStatus.ACTIVE
              AND s.expiredAt <= :now
            """)
    List<SeatHold> findExpiredActiveHolds(
            @Param("sessionId") Long sessionId,
            @Param("seatIds") List<Long> seatIds,
            @Param("now") LocalDateTime now
    );

    // US-12 Hold Seats — check for non-expired ACTIVE holds on the requested seats.
    // Used to detect SEAT_ALREADY_HELD before attempting insert.
    @Query("""
            SELECT s.seat.id FROM SeatHold s
            WHERE s.eventSession.id = :sessionId
              AND s.seat.id IN :seatIds
              AND s.status = com.dthxhieu.ticket_booking_system_be.common.enums.SeatHoldStatus.ACTIVE
              AND s.expiredAt > :now
            """)
    List<Long> findActiveHeldSeatIdsForSeats(
            @Param("sessionId") Long sessionId,
            @Param("seatIds") List<Long> seatIds,
            @Param("now") LocalDateTime now
    );

    // US-12 Get My Hold — active, non-expired holds for a specific user + session.
    @Query("""
            SELECT s FROM SeatHold s
            WHERE s.user.id = :userId
              AND s.eventSession.id = :sessionId
              AND s.status = com.dthxhieu.ticket_booking_system_be.common.enums.SeatHoldStatus.ACTIVE
              AND s.expiredAt > :now
            """)
    List<SeatHold> findActiveHoldsByUserAndSession(
            @Param("userId") Long userId,
            @Param("sessionId") Long sessionId,
            @Param("now") LocalDateTime now
    );

    // US-12 Release Hold — used for ownership verification.
    // Returns the SeatHold only if it belongs to the specified user.
    Optional<SeatHold> findByIdAndUserId(Long id, Long userId);

    // US-14: Cleanup SeatHolds after Payment SUCCESS.
    // MUST be scoped to: userId + eventSessionId + specific seatIds from BookingItems.
    // This prevents accidentally deleting SeatHolds of other users or unrelated seats.
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("""
            DELETE FROM SeatHold s
            WHERE s.user.id = :userId
              AND s.eventSession.id = :eventSessionId
              AND s.seat.id IN :seatIds
            """)
    void deleteByUserIdAndEventSessionIdAndSeatIdIn(
            @Param("userId") Long userId,
            @Param("eventSessionId") Long eventSessionId,
            @Param("seatIds") List<Long> seatIds
    );
}
