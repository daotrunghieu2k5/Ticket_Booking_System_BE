package com.dthxhieu.ticket_booking_system_be.venue.dto.response;

import com.dthxhieu.ticket_booking_system_be.common.enums.SeatType;
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
public class SeatResponse {

    private Long id;

    // venueId included in all responses for client convenience.
    private Long venueId;

    private String rowName;

    private Integer seatNumber;

    // seatCode is computed (rowName + seatNumber) — not stored in DB.
    // e.g. rowName="A", seatNumber=1 → seatCode="A1"
    private String seatCode;

    private SeatType seatType;

    private boolean active;
}
