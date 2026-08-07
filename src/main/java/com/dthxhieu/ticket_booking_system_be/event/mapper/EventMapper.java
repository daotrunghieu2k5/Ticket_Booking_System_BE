package com.dthxhieu.ticket_booking_system_be.event.mapper;

import com.dthxhieu.ticket_booking_system_be.entity.event.Event;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.CategorySummary;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventDetailResponse;
import com.dthxhieu.ticket_booking_system_be.event.dto.response.EventSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    // Maps Event to a lightweight summary for the paginated list view (§6.1).
    // categoryName is denormalized here to avoid triggering a lazy load per event.
    public EventSummaryResponse toSummary(Event event) {
        return EventSummaryResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .posterUrl(event.getPosterUrl())
                .categoryName(event.getCategory().getName())
                .status(event.getStatus())
                .build();
    }

    // Maps Event to a full detail response including nested category object (§6.2).
    public EventDetailResponse toDetail(Event event) {
        CategorySummary categorySummary = CategorySummary.builder()
                .id(event.getCategory().getId())
                .name(event.getCategory().getName())
                .build();

        return EventDetailResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .posterUrl(event.getPosterUrl())
                .bannerUrl(event.getBannerUrl())
                .saleStartTime(event.getSaleStartTime())
                .saleEndTime(event.getSaleEndTime())
                .status(event.getStatus())
                .category(categorySummary)
                .build();
    }
}
