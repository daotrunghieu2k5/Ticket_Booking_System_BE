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

    // Event title — maps to the `title` column per DATABASE.md §5.4.
    @Column(nullable = false)
    private String title;

    // Event description — optional long text.
    @Column(columnDefinition = "TEXT")
    private String description;

    // Poster image URL.
    @Column(length = 500)
    private String poster;

    // Duration stored in minutes per DATABASE.md §5.4.
    @Column(nullable = false)
    private Integer duration;

    // Minimum age requirement. Defaults to 0 (no age restriction).
    @Column(nullable = false)
    private Integer ageLimit;

    // Event lifecycle status stored as VARCHAR per project enum convention.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventStatus status;

    // category_id FK — also enables EventRepository.existsByCategoryId(Long)
    // for the delete-safety check in CategoryService (US-07).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
