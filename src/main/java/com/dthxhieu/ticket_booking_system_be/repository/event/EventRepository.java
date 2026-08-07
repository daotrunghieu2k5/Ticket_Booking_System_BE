package com.dthxhieu.ticket_booking_system_be.repository.event;

import com.dthxhieu.ticket_booking_system_be.common.enums.EventStatus;
import com.dthxhieu.ticket_booking_system_be.entity.event.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    // US-07: Used in CategoryServiceImpl delete-safety check.
    boolean existsByCategoryId(Long categoryId);

    // US-10: Duplicate title check on create (BR-02).
    boolean existsByTitleIgnoreCase(String title);

    // US-10: Duplicate title check on update, excluding the current event (BR-02).
    boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);

    // US-10: Combined search + filter with pagination (FR-03, FR-04, FR-05).
    // Using JPQL @Query to combine optional keyword/categoryId/status filters cleanly
    // without generating a cartesian product of derived method names.
    // All parameters are optional: null values are ignored via IS NULL OR conditions.
    @Query("""
            SELECT e FROM Event e
            WHERE (:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR e.category.id = :categoryId)
              AND (:status IS NULL OR e.status = :status)
            ORDER BY e.createdAt DESC
            """)
    Page<Event> findByFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") EventStatus status,
            Pageable pageable
    );
}
