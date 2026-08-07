package com.dthxhieu.ticket_booking_system_be.venue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class UpdateVenueRequest {

    @NotBlank(message = "Venue name is required")
    @Size(max = 255, message = "Venue name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Venue address is required")
    @Size(max = 500, message = "Venue address must not exceed 500 characters")
    private String address;

    // BR-05: new capacity must be >= current seat count. Checked in service layer.
    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than zero")
    private Integer capacity;
}
