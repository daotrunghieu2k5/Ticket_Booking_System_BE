package com.dthxhieu.ticket_booking_system_be.entity.event;

import com.dthxhieu.ticket_booking_system_be.common.enums.EventStatus;
import com.dthxhieu.ticket_booking_system_be.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event")
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Title must be unique (case-insensitive). Trimmed before save (BR-02, BR-03).
    @Column(nullable = false, length = 255)
    private String title;

    // Required long-form description (BR-06). TEXT column to allow up to 5000+ chars.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // Poster image URL (BR-07). DB column name is `poster` per V9 migration.
    @Column(name = "poster", nullable = false, length = 500)
    private String posterUrl;

    // Optional banner image URL (BR-08). Added in V16 migration.
    @Column(name = "banner", length = 500)
    private String bannerUrl;

    // Duration in minutes (DATABASE.md §5.4). Not exposed in US-10 API — retained for future use.
    @Column(nullable = false)
    private Integer duration;

    // Minimum age requirement (DATABASE.md §5.4). Not exposed in US-10 API — retained for future use.
    @Column(name = "age_limit", nullable = false)
    private Integer ageLimit;

    // Ticket sale window start (BR-09: must be before saleEndTime). Added in V16.
    @Column(name = "sale_start_time")
    private LocalDateTime saleStartTime;

    // Ticket sale window end (BR-09). Added in V16.
    @Column(name = "sale_end_time")
    private LocalDateTime saleEndTime;

    // Event lifecycle status. ACTIVE = visible to public; INACTIVE = soft-deleted (BR-10).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventStatus status;

    // FK to Category. Category must exist and (ideally) be ACTIVE before save (BR-04, BR-05).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
