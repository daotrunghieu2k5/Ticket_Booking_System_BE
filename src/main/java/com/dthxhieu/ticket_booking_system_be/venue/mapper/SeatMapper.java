package com.dthxhieu.ticket_booking_system_be.venue.mapper;

import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.SeatResponse;
import org.springframework.stereotype.Component;

@Component
public class SeatMapper {

    // Maps Seat entity to SeatResponse DTO.
    // seatCode is computed here (rowName + seatNumber) — it is not stored in the database.
    // This keeps the DB schema clean while giving clients a convenient display code.
    public SeatResponse toSeatResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .venueId(seat.getVenue().getId())
                .rowName(seat.getRowName())
                .seatNumber(seat.getSeatNumber())
                .seatCode(seat.getRowName() + seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .active(seat.isActive())
                .build();
    }
}
