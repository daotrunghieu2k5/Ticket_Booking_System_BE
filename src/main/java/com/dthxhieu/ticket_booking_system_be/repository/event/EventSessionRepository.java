package com.dthxhieu.ticket_booking_system_be.repository.event;

import com.dthxhieu.ticket_booking_system_be.common.enums.SessionStatus;
import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventSessionRepository extends JpaRepository<EventSession, Long> {

    // US-08: Check if any event session references this venue before deletion.
    boolean existsByVenueId(Long venueId);

    // US-10: Check if any event session belongs to an event before deletion.
    // If true, the event must be soft-deleted (INACTIVE) instead of physically deleted.
    boolean existsByEventId(Long eventId);

    // US-11: Retrieve sessions with optional filters for eventId, venueId, and status.
    // @Query used here because all three parameters are optional — null values are skipped
    // via IS NULL OR conditions, avoiding a cartesian explosion of derived method names.
    @Query("""
            SELECT s FROM EventSession s
            WHERE (:eventId IS NULL OR s.event.id = :eventId)
              AND (:venueId IS NULL OR s.venue.id = :venueId)
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.startTime ASC
            """)
    List<EventSession> findByFilters(
            @Param("eventId") Long eventId,
            @Param("venueId") Long venueId,
            @Param("status") SessionStatus status
    );

    // US-11 BR-08: Check for overlapping sessions at the same Venue on CREATE.
    // Two sessions A and B overlap if: A.start < B.end AND B.start < A.end.
    // This cannot be expressed with a derived query method (two-sided range check).
    @Query("""
            SELECT COUNT(s) > 0 FROM EventSession s
            WHERE s.venue.id = :venueId
              AND s.startTime < :endTime
              AND s.endTime > :startTime
            """)
    boolean existsOverlappingSession(
            @Param("venueId") Long venueId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // US-11 BR-08: Overlap check on UPDATE — excludes the session being updated
    // to prevent self-conflict detection (a session does not overlap with itself).
    @Query("""
            SELECT COUNT(s) > 0 FROM EventSession s
            WHERE s.venue.id = :venueId
              AND s.startTime < :endTime
              AND s.endTime > :startTime
              AND s.id <> :excludedId
            """)
    boolean existsOverlappingSessionExcluding(
            @Param("venueId") Long venueId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedId") Long excludedId
    );
}
