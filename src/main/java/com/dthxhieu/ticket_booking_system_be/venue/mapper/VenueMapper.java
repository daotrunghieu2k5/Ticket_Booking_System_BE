package com.dthxhieu.ticket_booking_system_be.venue.mapper;

import com.dthxhieu.ticket_booking_system_be.entity.venue.Venue;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.VenueResponse;
import org.springframework.stereotype.Component;

@Component
public class VenueMapper {

    // Maps the Venue entity to VenueResponse DTO.
    // @Component allows injection into VenueServiceImpl — same pattern as CategoryMapper.
    public VenueResponse toVenueResponse(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .capacity(venue.getCapacity())
                .build();
    }
}
