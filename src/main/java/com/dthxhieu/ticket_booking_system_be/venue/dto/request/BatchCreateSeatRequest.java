package com.dthxhieu.ticket_booking_system_be.venue.dto.request;

import com.dthxhieu.ticket_booking_system_be.common.enums.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
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
public class BatchCreateSeatRequest {

    // startRow and endRow define the row range (e.g. "A" to "J").
    // Must be alphabetic only. endRow >= startRow is validated in the service.
    @NotBlank(message = "Start row is required")
    @Pattern(regexp = "[A-Za-z]+", message = "Start row must contain only alphabetic characters")
    private String startRow;

    @NotBlank(message = "End row is required")
    @Pattern(regexp = "[A-Za-z]+", message = "End row must contain only alphabetic characters")
    private String endRow;

    // Seat number range. endSeatNumber >= startSeatNumber validated in service.
    @NotNull(message = "Start seat number is required")
    @Positive(message = "Start seat number must be greater than zero")
    private Integer startSeatNumber;

    @NotNull(message = "End seat number is required")
    @Positive(message = "End seat number must be greater than zero")
    private Integer endSeatNumber;

    @NotNull(message = "Seat type is required")
    private SeatType seatType;
}
