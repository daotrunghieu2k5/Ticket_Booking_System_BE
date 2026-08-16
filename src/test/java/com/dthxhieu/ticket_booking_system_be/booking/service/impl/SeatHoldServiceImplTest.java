package com.dthxhieu.ticket_booking_system_be.booking.service.impl;

import com.dthxhieu.ticket_booking_system_be.booking.dto.request.HoldSeatsRequest;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.SeatHoldResponse;
import com.dthxhieu.ticket_booking_system_be.common.enums.SessionStatus;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.booking.SeatHold;
import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Venue;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.BookingItemRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.SeatHoldRepository;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventSessionRepository;
import com.dthxhieu.ticket_booking_system_be.repository.venue.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatHoldServiceImplTest {

    @Mock
    private EventSessionRepository eventSessionRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private BookingItemRepository bookingItemRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SeatHoldServiceImpl seatHoldService;

    private User user;
    private Venue venue;
    private EventSession session;
    private Seat seat1, seat2, seat3;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).build();
        venue = Venue.builder().id(100L).build();
        
        session = EventSession.builder()
                .id(1L)
                .venue(venue)
                .status(SessionStatus.BOOKING_OPEN)
                .build();
                
        seat1 = Seat.builder().id(10L).venue(venue).build();
        seat2 = Seat.builder().id(20L).venue(venue).build();
        seat3 = Seat.builder().id(30L).venue(venue).build();
    }

    @Test
    void holdSeats_withMultipleSeats_shouldReturnAllSeatHoldIds() {
        // Arrange
        Long sessionId = 1L;
        Long userId = 1L;
        List<Long> requestedSeatIds = List.of(10L, 20L, 30L);
        HoldSeatsRequest request = HoldSeatsRequest.builder().seatIds(requestedSeatIds).build();

        when(eventSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(seatRepository.findAllById(requestedSeatIds)).thenReturn(List.of(seat1, seat2, seat3));
        when(bookingItemRepository.findBookedSeatIdsByEventSessionId(sessionId)).thenReturn(List.of());
        when(seatHoldRepository.findExpiredActiveHolds(anyLong(), anyList(), any(LocalDateTime.class))).thenReturn(List.of());
        when(seatHoldRepository.findActiveHeldSeatIdsForSeats(anyLong(), anyList(), any(LocalDateTime.class))).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Mock saving SeatHolds and assign IDs to them
        when(seatHoldRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<SeatHold> holds = invocation.getArgument(0);
            long idCounter = 123L;
            for (SeatHold hold : holds) {
                hold.setId(idCounter++);
            }
            return holds;
        });

        // Act
        SeatHoldResponse response = seatHoldService.holdSeats(sessionId, request, userId);

        // Assert
        assertNotNull(response);
        assertEquals(123L, response.getHoldId());
        assertEquals(3, response.getSeatHoldIds().size());
        assertEquals(List.of(123L, 124L, 125L), response.getSeatHoldIds());
        assertEquals(List.of(10L, 20L, 30L), response.getSeatIds());
        assertEquals(1L, response.getEventSessionId());
    }
}
