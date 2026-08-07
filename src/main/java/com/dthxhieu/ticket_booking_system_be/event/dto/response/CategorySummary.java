package com.dthxhieu.ticket_booking_system_be.event.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Nested DTO for category info inside EventDetailResponse.
// Only id and name are needed per the API contract §6.2.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummary {

    private Long id;

    private String name;
}
