package com.dthxhieu.ticket_booking_system_be.venue.service.impl;

import com.dthxhieu.ticket_booking_system_be.common.enums.SeatType;
import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Venue;
import com.dthxhieu.ticket_booking_system_be.repository.booking.BookingItemRepository;
import com.dthxhieu.ticket_booking_system_be.repository.venue.SeatRepository;
import com.dthxhieu.ticket_booking_system_be.repository.venue.VenueRepository;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.BatchCreateSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.BatchDeleteSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.CreateSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.request.UpdateSeatRequest;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.BatchCreateSeatResponse;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.BatchDeleteSeatResponse;
import com.dthxhieu.ticket_booking_system_be.venue.dto.response.SeatResponse;
import com.dthxhieu.ticket_booking_system_be.venue.mapper.SeatMapper;
import com.dthxhieu.ticket_booking_system_be.venue.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final VenueRepository venueRepository;
    private final BookingItemRepository bookingItemRepository;
    private final SeatMapper seatMapper;

    // --- Price multiplier defaults per SeatType ---
    // Not sent in request — the service assigns defaults so the API stays clean.
    // These can be made configurable (e.g. via application.yml) in the future.
    private static final BigDecimal MULTIPLIER_STANDARD    = BigDecimal.valueOf(1.00);
    private static final BigDecimal MULTIPLIER_VIP         = BigDecimal.valueOf(1.50);
    private static final BigDecimal MULTIPLIER_COUPLE      = BigDecimal.valueOf(1.20);
    private static final BigDecimal MULTIPLIER_WHEELCHAIR  = BigDecimal.valueOf(1.00);

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByVenue(Long venueId) {
        // Validate venue existence before loading seats (FR-09).
        if (!venueRepository.existsById(venueId)) {
            throw new ResourceNotFoundException("Venue not found.");
        }

        return seatRepository.findByVenueIdOrderByRowNameAscSeatNumberAsc(venueId)
                .stream()
                .map(seatMapper::toSeatResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getSeatById(Long id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found."));

        return seatMapper.toSeatResponse(seat);
    }

    @Override
    public SeatResponse createSeat(Long venueId, CreateSeatRequest request) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found."));

        // Normalize rowName: trim and convert to uppercase (BR-02, validation rule §7.1).
        String rowName = request.getRowName().trim().toUpperCase();

        // BR-04: (venue, row, seatNumber) must be unique.
        if (seatRepository.existsByVenueIdAndRowNameIgnoreCaseAndSeatNumber(venueId, rowName, request.getSeatNumber())) {
            throw new BusinessException(
                    "Seat " + rowName + request.getSeatNumber() + " already exists in this venue."
            );
        }

        Seat seat = Seat.builder()
                .venue(venue)
                .rowName(rowName)
                .seatNumber(request.getSeatNumber())
                .seatType(request.getSeatType())
                .priceMultiplier(resolveMultiplier(request.getSeatType()))
                .active(true)
                .build();

        return seatMapper.toSeatResponse(seatRepository.save(seat));
    }

    @Override
    public BatchCreateSeatResponse batchCreateSeats(Long venueId, BatchCreateSeatRequest request) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found."));

        String startRow = request.getStartRow().trim().toUpperCase();
        String endRow   = request.getEndRow().trim().toUpperCase();

        // Business validation: endRow must not be before startRow (§7.3).
        if (endRow.compareToIgnoreCase(startRow) < 0) {
            throw new BusinessException("End row must not be before start row.");
        }

        // Business validation: endSeatNumber must be >= startSeatNumber (§7.3).
        if (request.getEndSeatNumber() < request.getStartSeatNumber()) {
            throw new BusinessException("End seat number must be greater than or equal to start seat number.");
        }

        // Generate all (rowName, seatNumber) combinations.
        List<String> rows = generateRows(startRow, endRow);
        List<Seat> seatsToCreate = new ArrayList<>();

        // BR-09: Detect duplicates within the request itself.
        Set<String> requested = new HashSet<>();
        for (String row : rows) {
            for (int num = request.getStartSeatNumber(); num <= request.getEndSeatNumber(); num++) {
                String key = row + ":" + num;
                if (!requested.add(key)) {
                    // This cannot happen with range-based generation, but guard anyway.
                    throw new BusinessException("Duplicate seat " + row + num + " within the request.");
                }
                seatsToCreate.add(Seat.builder()
                        .venue(venue)
                        .rowName(row)
                        .seatNumber(num)
                        .seatType(request.getSeatType())
                        .priceMultiplier(resolveMultiplier(request.getSeatType()))
                        .active(true)
                        .build());
            }
        }

        // BR-10: Reject any seat that already exists in the database.
        // Check all before saving — atomicity (BR-14).
        for (Seat seat : seatsToCreate) {
            if (seatRepository.existsByVenueIdAndRowNameIgnoreCaseAndSeatNumber(
                    venueId, seat.getRowName(), seat.getSeatNumber())) {
                throw new BusinessException(
                        "Seat " + seat.getRowName() + seat.getSeatNumber() + " already exists in this venue."
                );
            }
        }

        // All checks passed — persist in one batch.
        List<Seat> saved = seatRepository.saveAll(seatsToCreate);

        return BatchCreateSeatResponse.builder()
                .createdCount(saved.size())
                .build();
    }

    @Override
    public SeatResponse updateSeat(Long id, UpdateSeatRequest request) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found."));

        String rowName = request.getRowName().trim().toUpperCase();

        // BR-06: New position must not conflict with another seat in the same venue,
        // excluding the seat being updated itself.
        if (seatRepository.existsByVenueIdAndRowNameIgnoreCaseAndSeatNumberAndIdNot(
                seat.getVenue().getId(), rowName, request.getSeatNumber(), id)) {
            throw new BusinessException(
                    "Seat " + rowName + request.getSeatNumber() + " already exists in this venue."
            );
        }

        seat.setRowName(rowName);
        seat.setSeatNumber(request.getSeatNumber());
        seat.setSeatType(request.getSeatType());
        seat.setPriceMultiplier(resolveMultiplier(request.getSeatType()));
        // active defaults to current value if not provided in request.
        if (request.getActive() != null) {
            seat.setActive(request.getActive());
        }

        return seatMapper.toSeatResponse(seatRepository.save(seat));
    }

    @Override
    public void deleteSeat(Long id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found."));

        validateSeatDeletion(seat);

        seatRepository.delete(seat);
    }

    @Override
    public BatchDeleteSeatResponse batchDeleteSeats(BatchDeleteSeatRequest request) {
        List<Long> seatIds = request.getSeatIds();

        // Validate no duplicate IDs in the request (§7.4).
        Set<Long> uniqueIds = new HashSet<>(seatIds);
        if (uniqueIds.size() != seatIds.size()) {
            throw new BusinessException("Duplicate seat IDs are not allowed in a batch delete request.");
        }

        // BR-11: Validate ALL seats before deleting any (atomic operation).
        // Collect all seats first, then run validation, then delete.
        List<Seat> seats = new ArrayList<>();
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seatId));
            seats.add(seat);
        }

        // Run all delete validations before touching any record.
        seats.forEach(this::validateSeatDeletion);

        // All validations passed — delete atomically within this transaction.
        seatRepository.deleteAllById(seatIds);

        return BatchDeleteSeatResponse.builder()
                .deletedCount(seatIds.size())
                .build();
    }

    // --- Private Helpers ---

    // Validates that a seat can be safely deleted.
    // Extracted to a shared method used by both single and batch delete.
    private void validateSeatDeletion(Seat seat) {
        // BR-07: Seat referenced by a BookingItem must not be deleted.
        // Deleting such a seat would corrupt booking history.
        if (bookingItemRepository.existsBySeatId(seat.getId())) {
            throw new BusinessException(
                    "Seat " + seat.getRowName() + seat.getSeatNumber() +
                    " cannot be deleted because it is referenced by an existing booking."
            );
        }

        // BR-08: Seat currently locked by SeatHold must not be deleted.
        // SeatHold is implemented in a future user story. Placeholder for future validation:
        // if (seatHoldRepository.existsBySeatId(seat.getId())) {
        //     throw new BusinessException("Seat is currently locked by a seat hold.");
        // }
    }

    // Returns the default price multiplier for a given SeatType.
    // Centralised so both single and batch create use the same defaults.
    private BigDecimal resolveMultiplier(SeatType seatType) {
        return switch (seatType) {
            case VIP        -> MULTIPLIER_VIP;
            case COUPLE     -> MULTIPLIER_COUPLE;
            case STANDARD   -> MULTIPLIER_STANDARD;
            case WHEELCHAIR -> MULTIPLIER_WHEELCHAIR;
        };
    }

    // Generates alphabetical row labels from startRow to endRow (inclusive).
    // Supports single-char rows (A-Z) and multi-char rows (AA-AZ, etc.)
    // using lexicographic ordering with alphabetical increment.
    //
    // Examples:
    //   "A" to "C"  → ["A", "B", "C"]
    //   "A" to "J"  → ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"]
    //   "AY" to "BB" → ["AY", "AZ", "BA", "BB"]
    private List<String> generateRows(String startRow, String endRow) {
        List<String> rows = new ArrayList<>();
        String current = startRow.toUpperCase();
        String end = endRow.toUpperCase();

        while (current.compareToIgnoreCase(end) <= 0) {
            rows.add(current);
            current = incrementRow(current);
        }
        return rows;
    }

    // Increments a row label alphabetically.
    // "A" → "B", "Z" → "AA", "AZ" → "BA", "ZZ" → "AAA"
    private String incrementRow(String row) {
        char[] chars = row.toCharArray();
        int i = chars.length - 1;

        while (i >= 0) {
            if (chars[i] < 'Z') {
                chars[i]++;
                return new String(chars);
            }
            chars[i] = 'A';
            i--;
        }
        // All chars were 'Z' — prepend an 'A' (e.g. "ZZ" → "AAA").
        return "A" + new String(chars);
    }
}
