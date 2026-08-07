package com.dthxhieu.ticket_booking_system_be.event.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Full event DTO for §6.2 Get Event Detail.
// Includes all fields including nested category object.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailResponse {

    private Long id;

    private String title;

    private String description;

    private String posterUrl;

    private String bannerUrl;

    private LocalDateTime saleStartTime;

    private LocalDateTime saleEndTime;

    private EventStatus status;

    // Nested category object per API contract §6.2.
    private CategorySummary category;
}
