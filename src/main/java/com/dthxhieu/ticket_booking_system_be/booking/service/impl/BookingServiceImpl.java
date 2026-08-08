package com.dthxhieu.ticket_booking_system_be.booking.service.impl;

import com.dthxhieu.ticket_booking_system_be.booking.dto.request.CreateBookingRequest;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.BookingItemResponse;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.BookingResponse;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.BookingSummaryResponse;
import com.dthxhieu.ticket_booking_system_be.booking.dto.response.PaymentSummaryResponse;
import com.dthxhieu.ticket_booking_system_be.booking.service.BookingService;
import com.dthxhieu.ticket_booking_system_be.common.enums.BookingItemStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.BookingStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.PaymentStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.SeatHoldStatus;
import com.dthxhieu.ticket_booking_system_be.common.enums.SessionStatus;
import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.booking.Booking;
import com.dthxhieu.ticket_booking_system_be.entity.booking.BookingItem;
import com.dthxhieu.ticket_booking_system_be.entity.booking.Payment;
import com.dthxhieu.ticket_booking_system_be.entity.booking.SeatHold;
import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.BookingItemRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.BookingRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.SeatHoldRepository;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final EventSessionRepository eventSessionRepository;
    private final UserRepository userRepository;

    // -----------------------------------------------------------------------
    // CREATE BOOKING — Atomic (BR-13), Validation-first
    // -----------------------------------------------------------------------

    @Override
    public BookingResponse createBooking(CreateBookingRequest request, Long userId) {
        // 1. Validate EventSession exists.
        EventSession session = eventSessionRepository.findById(request.getEventSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Event session not found."));

        // 2. BR-02: Session must not be CANCELLED or FINISHED.
        if (session.getStatus() == SessionStatus.CANCELLED
                || session.getStatus() == SessionStatus.FINISHED) {
            throw new BusinessException("This event session is not available for booking.");
        }

        // 3. BR-02: Current time must be within the booking window.
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getBookingOpen())) {
            throw new BusinessException("Booking has not opened yet for this event session.");
        }
        if (now.isAfter(session.getBookingClose())) {
            throw new BusinessException("Booking has closed for this event session.");
        }

        // 4. Validate no duplicate seatHoldIds in the request.
        List<Long> seatHoldIds = request.getSeatHoldIds();
        Set<Long> uniqueHoldIds = new HashSet<>(seatHoldIds);
        if (uniqueHoldIds.size() != seatHoldIds.size()) {
            throw new BusinessException("Duplicate seat hold IDs are not allowed.");
        }

        // 5. Load all requested SeatHold records.
        List<SeatHold> holds = seatHoldRepository.findAllById(seatHoldIds);
        if (holds.size() != seatHoldIds.size()) {
            throw new ResourceNotFoundException("One or more seat holds do not exist.");
        }

        // 6. BR-03: All holds must belong to the current user (ownership check).
        boolean ownershipViolation = holds.stream()
                .anyMatch(h -> !h.getUser().getId().equals(userId));
        if (ownershipViolation) {
            throw new BusinessException("You can only book seats from your own holds.");
        }

        // 7. BR-03: All holds must be ACTIVE and not expired.
        boolean anyExpiredOrInactive = holds.stream()
                .anyMatch(h -> h.getStatus() != SeatHoldStatus.ACTIVE
                        || h.getExpiredAt().isBefore(now)
                        || h.getExpiredAt().isEqual(now));
        if (anyExpiredOrInactive) {
            throw new BusinessException("One or more seat holds have expired or are no longer active.");
        }

        // 8. BR-04: All holds must be for the requested EventSession.
        boolean sessionMismatch = holds.stream()
                .anyMatch(h -> !h.getEventSession().getId().equals(session.getId()));
        if (sessionMismatch) {
            throw new BusinessException("All seat holds must belong to the requested event session.");
        }

        // 9. BR-04: All seats must belong to the session's Venue.
        Long sessionVenueId = session.getVenue().getId();
        boolean venueMismatch = holds.stream()
                .anyMatch(h -> !h.getSeat().getVenue().getId().equals(sessionVenueId));
        if (venueMismatch) {
            throw new BusinessException("One or more seats do not belong to the venue of this event session.");
        }

        // 10. BR-05: Batch check — none of the seats must already be booked for this session.
        List<Long> seatIds = holds.stream().map(h -> h.getSeat().getId()).toList();
        Set<Long> bookedSeatIds = new HashSet<>(
                bookingItemRepository.findBookedSeatIdsByEventSessionId(session.getId()));
        boolean alreadyBooked = seatIds.stream().anyMatch(bookedSeatIds::contains);
        if (alreadyBooked) {
            throw new BusinessException("One or more seats are already booked for this event session.");
        }

        // 11. All validation passed — calculate prices (BR-06).
        // seatPrice = eventSession.basePrice × seat.priceMultiplier (HALF_UP, scale 2).
        List<Seat> seats = holds.stream().map(SeatHold::getSeat).toList();
        List<BigDecimal> prices = seats.stream()
                .map(seat -> session.getBasePrice()
                        .multiply(seat.getPriceMultiplier())
                        .setScale(2, RoundingMode.HALF_UP))
                .toList();

        // 12. BR-07: Total = SUM(BookingItem.price) — backend calculated.
        BigDecimal totalAmount = prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 13. BR-08: Generate unique booking code.
        String bookingCode = generateUniqueBookingCode();

        // 14. Load authenticated user.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // 15. Build Booking.
        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .totalAmount(totalAmount)
                // WAITING_PAYMENT: Payment just created as PENDING, waiting for gateway (§6.1).
                .status(BookingStatus.WAITING_PAYMENT)
                .user(user)
                .eventSession(session)
                .build();

        // 16. Build BookingItems — BR-09: eventSession must match booking.eventSession.
        List<BookingItem> bookingItems = buildBookingItems(booking, holds, seats, prices, session);
        booking.setItems(bookingItems);

        // 17. Build Payment — BR-10: PENDING, amount = totalAmount.
        Payment payment = Payment.builder()
                .amount(totalAmount)
                .status(PaymentStatus.PENDING)
                .paymentMethod(null)  // gateway not selected yet
                .booking(booking)
                .build();
        booking.setPayment(payment);

        // 18. Save Booking (cascades to BookingItems + Payment via CascadeType.ALL).
        // UNIQUE(event_session_id, seat_id) is the final concurrency protection.
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            // Another concurrent booking captured the same seat(s) between our check and insert.
            // @Transactional ensures full rollback — no partial Booking remains.
            throw new BusinessException("One or more seats were just booked by another user. Please try again.");
        }

        // 19. BR-11: Delete SeatHolds after successful BookingItem creation.
        // Done AFTER save+flush to ensure BookingItems are persisted before holds are removed.
        seatHoldRepository.deleteAll(holds);

        return toBookingResponse(booking);
    }

    // -----------------------------------------------------------------------
    // GET MY BOOKINGS — Read-only, summary only (no items/payment to avoid N+1)
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toBookingSummaryResponse)
                .toList();
    }

    // -----------------------------------------------------------------------
    // GET BOOKING BY ID — Ownership enforced in service (FR-16 / BR-14)
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> {
                    // Distinguish "not found" from "not owner" by checking existence separately.
                    boolean exists = bookingRepository.existsById(bookingId);
                    return exists
                            ? new BusinessException("You do not have permission to view this booking.")
                            : new ResourceNotFoundException("Booking not found.");
                });

        return toBookingResponse(booking);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private List<BookingItem> buildBookingItems(
            Booking booking,
            List<SeatHold> holds,
            List<Seat> seats,
            List<BigDecimal> prices,
            EventSession session
    ) {
        List<BookingItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < holds.size(); i++) {
            Seat seat = seats.get(i);
            BigDecimal price = prices.get(i);

            // qr_code placeholder — real QR content generated in a future Ticket US.
            String qrCode = UUID.randomUUID().toString();

            // event_snapshot: minimal JSON snapshot of key data at booking time.
            // Ensures historical records are unaffected by future Event/Session changes.
            String snapshot = buildEventSnapshot(session, seat, price);

            items.add(BookingItem.builder()
                    .qrCode(qrCode)
                    .price(price)
                    .status(BookingItemStatus.VALID)
                    .eventSnapshot(snapshot)
                    .booking(booking)
                    .seat(seat)
                    .eventSession(session)  // BR-09: must equal booking.eventSession
                    .build());
        }
        return items;
    }

    private String buildEventSnapshot(EventSession session, Seat seat, BigDecimal price) {
        // Minimal JSON — structured so future US can parse it.
        return "{\"eventSessionId\":" + session.getId()
                + ",\"seatId\":" + seat.getId()
                + ",\"seatCode\":\"" + seat.getRowName() + seat.getSeatNumber() + "\""
                + ",\"price\":" + price
                + "}";
    }

    // BR-08: Generate a unique booking code. Format: BK-YYYYMMDD-XXXXXXXX.
    // Retries on rare collision (UNIQUE constraint in DB is the final guard).
    private String generateUniqueBookingCode() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "BK-" + date + "-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8).toUpperCase();
            if (!bookingRepository.existsByBookingCode(code)) {
                return code;
            }
        }
        throw new BusinessException("Failed to generate a unique booking code. Please try again.");
    }

    private BookingResponse toBookingResponse(Booking booking) {
        List<BookingItemResponse> itemResponses = booking.getItems().stream()
                .map(item -> BookingItemResponse.builder()
                        .id(item.getId())
                        .seatId(item.getSeat().getId())
                        .seatCode(item.getSeat().getRowName() + item.getSeat().getSeatNumber())
                        .price(item.getPrice())
                        .status(item.getStatus())
                        .build())
                .toList();

        Payment payment = booking.getPayment();
        PaymentSummaryResponse paymentResponse = payment == null ? null
                : PaymentSummaryResponse.builder()
                        .id(payment.getId())
                        .amount(payment.getAmount())
                        .status(payment.getStatus())
                        .paymentMethod(payment.getPaymentMethod())
                        .build();

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .eventSessionId(booking.getEventSession().getId())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .items(itemResponses)
                .payment(paymentResponse)
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private BookingSummaryResponse toBookingSummaryResponse(Booking booking) {
        return BookingSummaryResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .eventSessionId(booking.getEventSession().getId())
                .status(booking.getStatus())
                .totalAmount(booking.getTotalAmount())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
