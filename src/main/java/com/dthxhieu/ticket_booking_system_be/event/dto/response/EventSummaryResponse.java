package com.dthxhieu.ticket_booking_system_be.event.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lightweight DTO for paginated event list (§6.1 API contract).
// Only includes fields needed for list display — detail fields fetched separately via §6.2.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryResponse {

    private Long id;

    private String title;

    private String posterUrl;

    // Category name denormalized here to avoid N+1 issues in the list view.
    private String categoryName;

    private EventStatus status;
}
