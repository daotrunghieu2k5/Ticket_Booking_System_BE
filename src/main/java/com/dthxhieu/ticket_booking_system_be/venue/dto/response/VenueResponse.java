package com.dthxhieu.ticket_booking_system_be.venue.dto.response;

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
public class VenueResponse {

    private Long id;

    private String name;

    private String address;

    private Integer capacity;
}
