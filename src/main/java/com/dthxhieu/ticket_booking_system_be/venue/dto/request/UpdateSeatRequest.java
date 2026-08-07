package com.dthxhieu.ticket_booking_system_be.venue.dto.request;

import com.dthxhieu.ticket_booking_system_be.common.enums.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class UpdateSeatRequest {

    @NotBlank(message = "Row name is required")
    @Size(max = 10, message = "Row name must not exceed 10 characters")
    @Pattern(regexp = "[A-Za-z]+", message = "Row name must contain only alphabetic characters")
    private String rowName;

    @NotNull(message = "Seat number is required")
    @Positive(message = "Seat number must be greater than zero")
    private Integer seatNumber;

    @NotNull(message = "Seat type is required")
    private SeatType seatType;

    // active can be updated (e.g., to deactivate a damaged seat).
    // Defaults to true if not provided — handled in service.
    private Boolean active;
}
