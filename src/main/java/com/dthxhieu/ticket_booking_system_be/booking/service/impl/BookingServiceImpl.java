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
import com.dthxhieu.ticket_booking_system_be.common.exception.ConflictException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ForbiddenException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.booking.Booking;
import com.dthxhieu.ticket_booking_system_be.entity.booking.BookingItem;
import com.dthxhieu.ticket_booking_system_be.entity.booking.Payment;
import com.dthxhieu.ticket_booking_system_be.entity.booking.SeatHold;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.event.EventSession;
import com.dthxhieu.ticket_booking_system_be.entity.venue.Seat;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.BookingItemRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.BookingRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.PaymentRepository;
import com.dthxhieu.ticket_booking_system_be.repository.booking.SeatHoldRepository;
import com.dthxhieu.ticket_booking_system_be.repository.event.EventSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final String BOOKING_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BOOKING_CODE_RANDOM_LENGTH = 6;
    private static final int BOOKING_CODE_MAX_RETRIES = 5;

    private final EventSessionRepository eventSessionRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    // ===========================================================================
    // CREATE BOOKING
    // ===========================================================================

    // FR-13 / BR-13: The entire flow must be atomic.
    // Any failure rolls back Booking, BookingItems, and Payment creation.
    // SeatHolds are NOT modified or deleted here — they remain ACTIVE until US-14.
    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Long userId) {

        // -----------------------------------------------------------------------
        // 1. Validate duplicate seatHoldIds in the request.
        // Jakarta Validation cannot detect list-level duplicates, so we check here.
        // -----------------------------------------------------------------------
        List<Long> seatHoldIds = request.getSeatHoldIds();
        Set<Long> uniqueIds = new HashSet<>(seatHoldIds);
        if (uniqueIds.size() != seatHoldIds.size()) {
            throw new BusinessException("Duplicate seat hold IDs are not allowed.");
        }

        // -----------------------------------------------------------------------
        // 2. Load and validate EventSession.
        // BR-02: Session must exist and be in BOOKING_OPEN state with a valid booking window.
        // -----------------------------------------------------------------------
        EventSession session = eventSessionRepository.findById(request.getEventSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Event session not found."));

        LocalDateTime now = LocalDateTime.now();

        if (session.getStatus() != SessionStatus.BOOKING_OPEN) {
            throw new ConflictException("This event session is not currently accepting bookings.");
        }

        if (now.isBefore(session.getBookingOpen()) || now.isAfter(session.getBookingClose())) {
            throw new ConflictException("Booking window for this event session is not open.");
        }

        // -----------------------------------------------------------------------
        // 3. Load SeatHold records.
        // JpaRepository.findAllById() is inherited — no custom method needed.
        // -----------------------------------------------------------------------
        List<SeatHold> holds = seatHoldRepository.findAllById(seatHoldIds);
        if (holds.size() != seatHoldIds.size()) {
            throw new ResourceNotFoundException("One or more seat holds not found.");
        }

        // -----------------------------------------------------------------------
        // 4. Validate each SeatHold:
        //    BR-03: Must belong to current user.
        //    BR-03: Must be ACTIVE and not expired.
        //    Must belong to the requested EventSession.
        //    BR-04: Seat must belong to the session's Venue.
        // -----------------------------------------------------------------------
        Long sessionVenueId = session.getVenue().getId();

        for (SeatHold hold : holds) {
            // BR-03: Ownership check.
            if (!hold.getUser().getId().equals(userId)) {
                throw new ForbiddenException("One or more seat holds do not belong to you.");
            }

            // BR-03: Status and expiry check.
            if (hold.getStatus() != SeatHoldStatus.ACTIVE || !hold.getExpiredAt().isAfter(now)) {
                throw new ConflictException("One or more seat holds have expired or are no longer active.");
            }

            // EventSession consistency: all holds must be for the same session as the request.
            if (!hold.getEventSession().getId().equals(request.getEventSessionId())) {
                throw new ConflictException("One or more seat holds do not belong to the requested event session.");
            }

            // BR-04: Seat must belong to the session's Venue.
            if (!hold.getSeat().getVenue().getId().equals(sessionVenueId)) {
                throw new ConflictException("One or more seats do not belong to the venue of this event session.");
            }
        }

        // -----------------------------------------------------------------------
        // 5. Application-level check: reject seats already in a BookingItem.
        // This gives a clean 409 for the common case.
        // The DB UNIQUE(event_session_id, seat_id) constraint handles concurrent races.
        // -----------------------------------------------------------------------
        for (SeatHold hold : holds) {
            Long seatId = hold.getSeat().getId();
            if (bookingItemRepository.existsByEventSessionIdAndSeatId(session.getId(), seatId)) {
                throw new ConflictException("One or more seats are already booked for this event session.");
            }
        }

        // -----------------------------------------------------------------------
        // 6. Calculate prices.
        // Price formula confirmed from DATABASE.md and SeatHoldServiceImpl:
        //   price = eventSession.basePrice × seat.priceMultiplier (HALF_UP, scale 2)
        // -----------------------------------------------------------------------
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SeatHold hold : holds) {
            BigDecimal price = session.getBasePrice()
                    .multiply(hold.getSeat().getPriceMultiplier())
                    .setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(price);
        }

        // -----------------------------------------------------------------------
        // 7. Generate a unique booking code.
        // Format: BK-{yyyyMMdd}-{6 random alphanumeric chars}
        // The DB UNIQUE constraint on booking_code is the final safety net.
        // -----------------------------------------------------------------------
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        String bookingCode = generateUniqueBookingCode();

        // -----------------------------------------------------------------------
        // 8–10. Persist Booking, BookingItems, and Payment atomically.
        // The @Transactional annotation ensures all-or-nothing — any failure rolls back.
        // -----------------------------------------------------------------------
        try {
            // 8. Create and save Booking.
            Booking booking = Booking.builder()
                    .bookingCode(bookingCode)
                    .totalAmount(totalAmount)
                    .status(BookingStatus.WAITING_PAYMENT)
                    .user(user)
                    .eventSession(session)
                    .build();
            bookingRepository.save(booking);

            // 9. Create and save BookingItems.
            // BR-09: booking_item.event_session_id must equal booking.event_session_id.
            // qrCode is null — generated after payment confirmation (US-14).
            List<BookingItem> items = holds.stream()
                    .map(hold -> {
                        Seat seat = hold.getSeat();
                        BigDecimal price = session.getBasePrice()
                                .multiply(seat.getPriceMultiplier())
                                .setScale(2, RoundingMode.HALF_UP);
                        return BookingItem.builder()
                                .qrCode(null)
                                .price(price)
                                .status(BookingItemStatus.VALID)
                                .eventSnapshot(null)
                                .booking(booking)
                                .seat(seat)
                                .eventSession(session)
                                .build();
                    })
                    .toList();
            bookingItemRepository.saveAll(items);

            // 10. Create Payment with PENDING status.
            // BR-10: Payment amount must equal Booking total.
            // paymentMethod is null — user selects gateway when initiating payment (US-14).
            Payment payment = Payment.builder()
                    .amount(totalAmount)
                    .paymentMethod(null)
                    .status(PaymentStatus.PENDING)
                    .booking(booking)
                    .build();
            paymentRepository.save(payment);

            // NOTE: SeatHolds are NOT deleted or modified here.
            // They remain ACTIVE so the seat stays "reserved" until payment is confirmed.
            // US-14 will release/delete SeatHolds after Payment SUCCESS.

            return buildBookingResponse(booking, items, payment);

        } catch (DataIntegrityViolationException ex) {
            // A concurrent request won the race for one of the seats.
            // The UNIQUE(event_session_id, seat_id) constraint on booking_item caught it.
            throw new ConflictException("Seat is no longer available. Please try again.");
        }
    }

    // ===========================================================================
    // GET MY BOOKINGS
    // ===========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryResponse> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(booking -> BookingSummaryResponse.builder()
                        .id(booking.getId())
                        .bookingCode(booking.getBookingCode())
                        .eventSessionId(booking.getEventSession().getId())
                        .status(booking.getStatus())
                        .totalAmount(booking.getTotalAmount())
                        .createdAt(booking.getCreatedAt())
                        .build())
                .toList();
    }

    // ===========================================================================
    // GET BOOKING DETAIL
    // ===========================================================================

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingDetail(Long bookingId, Long userId) {
        // Load booking — 404 if not found.
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        // BR-14: Ownership check — 403 if booking belongs to a different user.
        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to view this booking.");
        }

        // Load items and payment via repositories (avoids LAZY collection initialization issues).
        List<BookingItem> items = bookingItemRepository.findByBookingId(bookingId);
        Payment payment = booking.getPayment();

        return buildBookingResponse(booking, items, payment);
    }

    // ===========================================================================
    // PRIVATE HELPERS
    // ===========================================================================

    // Build the full BookingResponse from persisted entities.
    private BookingResponse buildBookingResponse(Booking booking, List<BookingItem> items, Payment payment) {
        List<BookingItemResponse> itemResponses = items.stream()
                .map(item -> BookingItemResponse.builder()
                        .id(item.getId())
                        .seatId(item.getSeat().getId())
                        .seatCode(item.getSeat().getRowName() + item.getSeat().getSeatNumber())
                        .price(item.getPrice())
                        .status(item.getStatus())
                        .build())
                .toList();

        PaymentSummaryResponse paymentResponse = PaymentSummaryResponse.builder()
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

    // Generate a unique booking code in format BK-{yyyyMMdd}-{6 alphanum}.
    // Retries up to BOOKING_CODE_MAX_RETRIES times if a collision occurs.
    // The DB UNIQUE constraint is the final safety net even if all retries collide.
    private String generateUniqueBookingCode() {
        SecureRandom random = new SecureRandom();
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        for (int attempt = 0; attempt < BOOKING_CODE_MAX_RETRIES; attempt++) {
            StringBuilder sb = new StringBuilder("BK-").append(datePart).append("-");
            for (int i = 0; i < BOOKING_CODE_RANDOM_LENGTH; i++) {
                sb.append(BOOKING_CODE_CHARS.charAt(random.nextInt(BOOKING_CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!bookingRepository.existsByBookingCode(code)) {
                return code;
            }
        }
        // Extremely unlikely: all retries collided. The DB UNIQUE constraint will catch it.
        // The exception will be converted to a clean error by the DataIntegrityViolation handler.
        throw new BusinessException("Unable to generate a unique booking code. Please try again.");
    }
}
