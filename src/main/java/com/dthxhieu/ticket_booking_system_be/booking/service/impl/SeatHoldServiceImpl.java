package com.dthxhieu.ticket_booking_system_be.booking.service.impl;

import com.dthxhieu.ticket_booking_system_be.booking.dto.request.HoldSeatsRequest;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.SeatAvailabilityItem;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.SeatHoldResponse;
import com.dthxhieu.ticket_booking_system_be.booking.service.SeatHoldService;
import com.dthxhieu.ticket_booking_system_be.common.constants.SeatHoldConstants;
import com.dthxhieu.ticket_booking_system_be.common.enums.SeatAvailabilityStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.SeatHoldStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.SessionStatus;
import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.booking.SeatHold;
import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.BookingItemRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.SeatHoldRepository;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventSessionRepository;
import com.dthxhieu.ticket_booking_system_be.repository.venue.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatHoldServiceImpl implements SeatHoldService {

    private final EventSessionRepository eventSessionRepository;
    private final SeatRepository seatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BookingItemRepository bookingItemRepository;
    private final UserRepository userRepository;

    // -----------------------------------------------------------------------
    // GET SEAT MAP
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SeatAvailabilityItem> getSeatMap(Long sessionId) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Event session not found."));

        LocalDateTime now = LocalDateTime.now();

        // Load all seats for this venue — 1 query.
        List<Seat> seats = seatRepository.findByVenueIdOrderByRowNameAscSeatNumberAsc(
                session.getVenue().getId());

        // Batch: booked seat IDs — 1 query (no per-seat querying).
        Set<Long> bookedIds = new HashSet<>(
                bookingItemRepository.findBookedSeatIdsByEventSessionId(sessionId));

        // Batch: actively held seat IDs — 1 query.
        Set<Long> heldIds = new HashSet<>(
                seatHoldRepository.findActiveHeldSeatIds(sessionId, now));

        // Build response using O(1) Set lookups — no N+1.
        return seats.stream()
                .map(seat -> {
                    SeatAvailabilityStatus status;
                    if (bookedIds.contains(seat.getId()))    status = SeatAvailabilityStatus.BOOKED;
                    else if (heldIds.contains(seat.getId())) status = SeatAvailabilityStatus.HELD;
                    else                                      status = SeatAvailabilityStatus.AVAILABLE;

                    // Price formula: basePrice × priceMultiplier, HALF_UP to 2 decimal places.
                    // This value is the basis for BookingItem.price in US-13.
                    BigDecimal price = session.getBasePrice()
                            .multiply(seat.getPriceMultiplier())
                            .setScale(2, RoundingMode.HALF_UP);

                    // seatCode is a derived display value — rowName + seatNumber (e.g. "A1").
                    String seatCode = seat.getRowName() + seat.getSeatNumber();

                    return SeatAvailabilityItem.builder()
                            .id(seat.getId())
                            .seatCode(seatCode)
                            .rowName(seat.getRowName())
                            .seatType(seat.getSeatType())
                            .price(price)
                            .status(status)
                            .build();
                })
                .toList();
    }

    // -----------------------------------------------------------------------
    // HOLD SEATS — Validate everything first, then save
    // -----------------------------------------------------------------------

    @Override
    public SeatHoldResponse holdSeats(Long sessionId, HoldSeatsRequest request, Long userId) {
        // 1. Validate EventSession.
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Event session not found."));

        // BR-09: Session must not be CANCELLED or FINISHED.
        if (session.getStatus() == SessionStatus.CANCELLED
                || session.getStatus() == SessionStatus.FINISHED) {
            throw new BusinessException("This event session is not available for seat selection.");
        }

        List<Long> seatIds = request.getSeatIds();

        // 2. Validate no duplicate seatIds in the request (US-12 §7.1).
        Set<Long> uniqueIds = new HashSet<>(seatIds);
        if (uniqueIds.size() != seatIds.size()) {
            throw new BusinessException("Duplicate seat IDs are not allowed.");
        }

        // 3. Load all requested Seat entities.
        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("One or more requested seats do not exist.");
        }

        // 4. BR-01 / BR-10: All seats must belong to the session's Venue.
        Long sessionVenueId = session.getVenue().getId();
        boolean venueMismatch = seats.stream()
                .anyMatch(seat -> !seat.getVenue().getId().equals(sessionVenueId));
        if (venueMismatch) {
            throw new BusinessException(
                    "One or more seats do not belong to the venue of this event session.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 5. BR-08: Batch check — reject if any seat is already booked.
        Set<Long> bookedIds = new HashSet<>(
                bookingItemRepository.findBookedSeatIdsByEventSessionId(sessionId));
        boolean anyBooked = seatIds.stream().anyMatch(bookedIds::contains);
        if (anyBooked) {
            throw new BusinessException("One or more requested seats are already booked.");
        }

        // 6. Cleanup: physically delete logically-expired ACTIVE holds for requested seats.
        // This frees the partial unique index slot for those seats so new holds can be inserted.
        List<SeatHold> expiredHolds = seatHoldRepository.findExpiredActiveHolds(sessionId, seatIds, now);
        if (!expiredHolds.isEmpty()) {
            seatHoldRepository.deleteAll(expiredHolds);
            seatHoldRepository.flush(); // ensure deletes are visible before insert check
        }

        // 7. BR-07: Batch check — reject if any seat has a non-expired ACTIVE hold.
        List<Long> alreadyHeldIds = seatHoldRepository.findActiveHeldSeatIdsForSeats(sessionId, seatIds, now);
        if (!alreadyHeldIds.isEmpty()) {
            throw new BusinessException("One or more requested seats are already held.");
        }

        // 8. All validation passed — build SeatHold records for all requested seats.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        LocalDateTime expiresAt = now.plusMinutes(SeatHoldConstants.HOLD_DURATION_MINUTES);
        String groupToken = UUID.randomUUID().toString(); // same token for all seats in this request

        List<SeatHold> holds = seats.stream()
                .map(seat -> SeatHold.builder()
                        .holdToken(groupToken)
                        .status(SeatHoldStatus.ACTIVE)
                        .expiredAt(expiresAt)
                        .user(user)
                        .eventSession(session)
                        .seat(seat)
                        .build())
                .toList();

        // 9. Save all atomically.
        // If two concurrent requests both reach this point for the same seat,
        // the partial unique index prevents both INSERTs from succeeding.
        // The loser receives a DataIntegrityViolationException — caught below.
        try {
            List<SeatHold> saved = seatHoldRepository.saveAll(holds);
            seatHoldRepository.flush();

            List<Long> seatHoldIds = saved.stream()
                    .map(SeatHold::getId)
                    .toList();

            Long holdId = seatHoldIds.get(0);

            return SeatHoldResponse.builder()
                    .holdId(holdId)
                    .seatHoldIds(seatHoldIds)
                    .eventSessionId(sessionId)
                    .seatIds(seatIds)
                    .expiresAt(expiresAt)
                    .build();

        } catch (DataIntegrityViolationException ex) {
            // A concurrent request won the race for at least one requested seat.
            // The @Transactional rollback ensures no partial hold remains.
            throw new BusinessException("One or more seats are no longer available.");
        }
    }

    // -----------------------------------------------------------------------
    // RELEASE HOLD
    // -----------------------------------------------------------------------

    @Override
    public void releaseHold(Long holdId, Long userId) {
        // Ownership-safe lookup — returns empty if hold belongs to a different user.
        SeatHold hold = seatHoldRepository.findByIdAndUserId(holdId, userId)
                .orElseThrow(() -> {
                    // Check if hold exists at all to give the right error message.
                    boolean exists = seatHoldRepository.existsById(holdId);
                    return exists
                            ? new BusinessException("You do not have permission to release this seat hold.")
                            : new ResourceNotFoundException("Seat hold not found.");
                });

        // Reject release of an already-expired hold.
        if (hold.getExpiredAt().isBefore(LocalDateTime.now())
                || hold.getExpiredAt().isEqual(LocalDateTime.now())) {
            throw new BusinessException("This seat hold has already expired.");
        }

        // Set status to CANCELLED — removes this row from the partial unique index
        // (which only covers status = 'ACTIVE'), making the seat immediately available.
        hold.setStatus(SeatHoldStatus.CANCELLED);
        seatHoldRepository.save(hold);
    }

    // -----------------------------------------------------------------------
    // GET MY ACTIVE HOLD
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SeatHoldResponse getMyActiveHold(Long sessionId, Long userId) {
        eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Event session not found."));

        List<SeatHold> activeHolds = seatHoldRepository.findActiveHoldsByUserAndSession(
                userId, sessionId, LocalDateTime.now());

        if (activeHolds.isEmpty()) {
            return null;
        }

        List<Long> heldSeatIds = activeHolds.stream()
                .map(h -> h.getSeat().getId())
                .toList();

        List<Long> seatHoldIds = activeHolds.stream()
                .map(SeatHold::getId)
                .toList();

        return SeatHoldResponse.builder()
                .holdId(seatHoldIds.get(0))
                .seatHoldIds(seatHoldIds)
                .eventSessionId(sessionId)
                .seatIds(heldSeatIds)
                .expiresAt(activeHolds.get(0).getExpiredAt())
                .build();
    }
}
