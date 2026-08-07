package com.dthxhieu.ticket_booking_system_be.event.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Wrapper for paginated event list per API contract §6.1.
// Encapsulates pagination metadata alongside content so the client
// does not need to parse Spring's Page object directly.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPageResponse {

    private List<EventSummaryResponse> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;
}
