US-13 — Booking Management
1. Objective

Implement the booking creation and booking management flow for the Ticket Booking System.

The feature allows an authenticated user to:

Create a booking from selected seats
Convert temporarily held seats into BookingItems
Calculate the booking total
Generate a unique booking code
Create the corresponding Payment record
View their bookings
View booking details

US-13 is implemented after:

US-11 — Event Session Management
US-12 — Seat Availability & Seat Hold

The booking flow must use SeatHold to ensure that users can only book seats they currently hold.

2. Scope
Included
Create booking from held seats
Validate current user ownership of SeatHold
Validate EventSession
Validate Seat
Validate seat availability
Create Booking
Create BookingItems
Calculate ticket price
Calculate total amount
Generate unique booking code
Create initial Payment with PENDING status
Delete/release SeatHold after successful booking creation
Get current user's bookings
Get booking detail
Prevent users from accessing another user's booking
Not Included
Payment gateway integration
VNPay integration
PayOS integration
MOMO integration
Payment callback/webhook
Refund
QR code generation/verification
Ticket scanning
Notification/email after booking
Admin booking management
Booking cancellation unless already required by the existing codebase

Payment processing belongs to later Payment functionality.

3. Business Flow
3.1 Create Booking
Client
  ↓
POST /api/v1/bookings
  ↓
Authenticate current user
  ↓
Validate request
  ↓
Validate EventSession
  ↓
Validate booking window
  ↓
Load SeatHolds
  ↓
Are all SeatHolds owned by current user?
  ├─ NO → BusinessException
  └─ YES
       ↓
Are all holds still active?
  ├─ NO → BusinessException
  └─ YES
       ↓
Validate Seats belong to EventSession Venue
       ↓
Validate seats are not already booked
       ↓
Calculate each ticket price
       ↓
Calculate total amount
       ↓
Generate unique booking code
       ↓
Create Booking
       ↓
Create BookingItems
       ↓
Create Payment(PENDING)
       ↓
Delete SeatHolds
       ↓
Commit transaction
       ↓
Return BookingResponse
3.2 Get My Bookings
Client
  ↓
GET /api/v1/bookings/me
  ↓
Authenticate current user
  ↓
Load bookings by current user
  ↓
Return booking list
3.3 Get Booking Detail
Client
  ↓
GET /api/v1/bookings/{bookingId}
  ↓
Authenticate current user
  ↓
Find booking
  ↓
Booking exists?
  ├─ NO → BusinessException
  └─ YES
       ↓
Booking belongs to current user?
       ├─ NO → BusinessException
       └─ YES
            ↓
Return booking detail
4. Functional Requirements
FR-01

Authenticated users can create a booking for an EventSession.

FR-02

A booking must contain at least one seat.

FR-03

A user can only book seats that are currently held by that user.

FR-04

The system must verify that all requested SeatHolds belong to the current user.

FR-05

The system must reject expired or inactive SeatHolds.

FR-06

The system must verify that the selected seats belong to the Venue of the EventSession.

FR-07

The system must prevent a seat from being booked more than once for the same EventSession.

The database already enforces:

UNIQUE(event_session_id, seat_id)

on booking_item.

FR-08

The system must calculate the final price of every BookingItem on the backend.

FR-09

The system must calculate:

Booking.total_amount
=
SUM(BookingItem.price)
FR-10

The system must generate a unique booking code.

FR-11

Creating a booking must create exactly one Payment with initial status:

PENDING
FR-12

SeatHolds must be removed after the Booking and BookingItems are successfully created.

FR-13

The booking creation process must execute within one transaction.

FR-14

Authenticated users can retrieve their own bookings.

FR-15

Authenticated users can retrieve details of their own booking.

FR-16

Users must not be able to access another user's booking.

FR-17

All API responses must use:

ApiResponse<T>
5. Business Rules
BR-01 — Authentication

Only authenticated users can create or view bookings.

The current user must be obtained from the Spring Security context.

The client must not provide userId as the source of ownership.

BR-02 — EventSession

The EventSession must exist.

The session must allow booking.

Booking is only allowed within:

booking_open
≤ current time
≤ booking_close

Booking is allowed only when:

session.status = BOOKING_OPEN

AND

bookingOpen <= now <= bookingClose
BR-03 — SeatHold ownership

Every seat being booked must have an active SeatHold owned by the current user.

A user cannot book:

another user's hold
an expired hold
a cancelled hold
a seat without a hold
BR-04 — Seat consistency

Every selected Seat must belong to:

EventSession.venue

The system must not allow a Seat from another Venue to be booked for the current EventSession.

This follows the database rule that Seats permanently belong to a Venue and EventSessions reuse the Seats of their Venue.

BR-05 — Seat availability

Before creating BookingItems:

No valid BookingItem exists

for the same:

event_session_id
seat_id

The database constraint:

UNIQUE(event_session_id, seat_id)

is the final protection against duplicate ticket sales.

BR-06 — Ticket price

The ticket price must be calculated on the backend.

Based on the current database model:

seatPrice
=
eventSession.basePrice
×
seat.priceMultiplier

The calculated value is stored in:

booking_item.price

The original Seat/EventSession pricing may change later, but the BookingItem price must remain unchanged.

BR-07 — Booking total

The total must be calculated from BookingItems:

totalAmount
=
Σ bookingItem.price

The frontend must never be trusted to provide the final total amount.

BR-08 — Booking code

Every Booking must have a unique:

booking_code

The database already defines:

UNIQUE(booking_code)

If a generated booking code conflicts with an existing code, generate another code.

BR-09 — BookingItem consistency

Every BookingItem must satisfy:

booking_item.booking.event_session_id
==
booking_item.event_session_id

The Service layer must guarantee this invariant.

BR-10 — Payment

Creating a Booking creates exactly one Payment.

Initial state:

Payment.status = PENDING

Payment amount:

Payment.amount
=
Booking.total_amount

Payment gateway processing is not part of US13.

The database defines a one-to-one Booking → Payment relationship.

BR-11 — SeatHold release

After Booking and BookingItems have been successfully created:

SeatHold
    ↓
Release SeatHold according to the actual US-12 implementation.

If US-12 uses physical deletion:
    DELETE SeatHold

If US-12 uses status-based release:
    UPDATE status

This prevents the same SeatHold from remaining active after the seat has become a BookingItem.

The database explicitly allows SeatHold records to be deleted after successful payment and requires expired holds not to block new bookings.

For the current booking flow, once the BookingItems are successfully persisted, the holds are no longer needed.

BR-12 — Booking history

Bookings must never be physically deleted.

The database explicitly requires booking history to be preserved.

BR-13 — Transaction consistency

Booking creation must be atomic.

Either all of these succeed:

Booking
BookingItems
Payment
SeatHold deletion

or none of them are committed.

If any operation fails, the transaction must roll back.

BR-14 — Ownership

A user can only view their own bookings.

The system must never trust:

userId

from the request.

Use the authenticated user.

6. API Contract
6.1 Create Booking
Endpoint
POST /api/v1/bookings
Authentication

Required.

Request
{
  "eventSessionId": 10,
  "seatHoldIds": [101, 102, 103]
}

The frontend sends SeatHold IDs created by US12.

Do not send:

userId
totalAmount
price
bookingCode

These values must be generated/calculated by the backend.

Success Response
{
  "success": true,
  "message": "Booking created successfully.",
  "data": {
    "id": 1001,
    "bookingCode": "BK-20260808-ABC123",
    "eventSessionId": 10,
    "status": "WAITING_PAYMENT",
    "totalAmount": 450000,
    "items": [
      {
        "id": 2001,
        "seatId": 101,
        "seatCode": "A01",
        "price": 150000,
        "status": "VALID"
      },
      {
        "id": 2002,
        "seatId": 102,
        "seatCode": "A02",
        "price": 150000,
        "status": "VALID"
      },
      {
        "id": 2003,
        "seatId": 103,
        "seatCode": "A03",
        "price": 150000,
        "status": "VALID"
      }
    ],
    "payment": {
      "id": 3001,
      "amount": 450000,
      "status": "PENDING",
      "paymentMethod": null
    }
  },
  "timestamp": "2026-08-08T10:00:00"
}

Important: The exact initial Booking status must follow the existing project/business flow. The database supports:

PENDING
WAITING_PAYMENT
PAID
COMPLETED
CANCELLED
EXPIRED

For this implementation, WAITING_PAYMENT is recommended because Payment has just been created as PENDING.

6.2 Get My Bookings
Endpoint
GET /api/v1/bookings/me
Authentication

Required.

Response
{
  "success": true,
  "message": "Bookings retrieved successfully.",
  "data": [
    {
      "id": 1001,
      "bookingCode": "BK-20260808-ABC123",
      "eventSessionId": 10,
      "status": "WAITING_PAYMENT",
      "totalAmount": 450000,
      "createdAt": "2026-08-08T10:00:00"
    }
  ],
  "timestamp": "2026-08-08T10:05:00"
}
6.3 Get Booking Detail
Endpoint
GET /api/v1/bookings/{bookingId}
Authentication

Required.

Response
{
  "success": true,
  "message": "Booking retrieved successfully.",
  "data": {
    "id": 1001,
    "bookingCode": "BK-20260808-ABC123",
    "eventSessionId": 10,
    "status": "WAITING_PAYMENT",
    "totalAmount": 450000,
    "items": [
      {
        "id": 2001,
        "seatId": 101,
        "seatCode": "A01",
        "price": 150000,
        "status": "VALID"
      }
    ],
    "payment": {
      "id": 3001,
      "amount": 450000,
      "status": "PENDING",
      "paymentMethod": null
    },
    "createdAt": "2026-08-08T10:00:00"
  },
  "timestamp": "2026-08-08T10:05:00"
}
6.4 Error Cases
SeatHold does not exist
404 NOT FOUND
SeatHold belongs to another user
403 FORBIDDEN

or the project's standard business-error mapping.

SeatHold expired
409 CONFLICT
Seat already booked
409 CONFLICT
EventSession unavailable
409 CONFLICT
Booking does not belong to current user
403 FORBIDDEN
Invalid request
400 BAD REQUEST
7. Validation Rules
CreateBookingRequest
eventSessionId
required
must not be null
must reference an existing EventSession
seatHoldIds
required
must not be empty
every ID must be valid
IDs must not be duplicated

Example:

{
  "eventSessionId": 10,
  "seatHoldIds": [101, 102, 103]
}

Valid.

{
  "eventSessionId": 10,
  "seatHoldIds": []
}

Invalid.

{
  "eventSessionId": 10,
  "seatHoldIds": [101, 101]
}

Invalid.

Do not validate in Controller

Jakarta Validation should handle request validation.

Business validation must remain inside Service:

EventSession status
Booking window
SeatHold ownership
SeatHold expiration
Seat/Venue consistency
Existing BookingItem
Price calculation

This follows the project convention that business rules belong in the Service layer.

8. Database Impact
Read
users
event_session
seat
seat_hold
booking_item
Insert
booking
booking_item
payment
Delete
seat_hold

Only the SeatHolds successfully converted into BookingItems should be removed.

No Schema Change Expected

US13 should not require a new table.

The current database already provides:

booking
booking_item
payment
seat_hold

with the required relationships.

Do not create:

ticket
booking_seat
order
reservation

tables.

9. Expected Classes
Controller
BookingController
Service
BookingService
BookingServiceImpl
DTO — Request
CreateBookingRequest
DTO — Response
BookingResponse
BookingItemResponse
BookingSummaryResponse
PaymentSummaryResponse

Use existing DTO naming conventions if equivalent DTOs already exist.

Repository
BookingRepository
BookingItemRepository
PaymentRepository
SeatHoldRepository
EventSessionRepository
SeatRepository

Only modify repositories when required.

Mapper
BookingMapper
BookingItemMapper

Reuse existing mapper infrastructure if available.

Entity

Existing:

Booking
BookingItem
Payment
SeatHold

No new entity should be created unless the current codebase is missing a database entity.

10. Transaction

Create Booking must be transactional:

@Transactional

Recommended flow:

Validate request
      ↓
Load EventSession
      ↓
Validate booking window
      ↓
Load SeatHolds
      ↓
Validate ownership
      ↓
Validate expiration/status
      ↓
Load Seats
      ↓
Validate Venue
      ↓
Check existing BookingItems
      ↓
Calculate prices
      ↓
Create Booking
      ↓
Create BookingItems
      ↓
Create Payment
      ↓
Delete SeatHolds
      ↓
Commit

If any step fails:

ROLLBACK

No partial Booking should remain.

Concurrency

The application-level check is not sufficient by itself.

The database constraint:

UNIQUE(event_session_id, seat_id)

on booking_item is the final protection against selling the same seat twice.

If concurrent requests cause a constraint violation:

DataIntegrityViolationException
        ↓
Rollback transaction
        ↓
Convert to BusinessException

Do not expose the raw database exception to the client.

This follows the database design where (event_session_id, seat_id) is explicitly defined as the unique constraint preventing duplicate seat sales.

11. Exception Cases

The Service must use BusinessException or the project's existing business-specific exceptions.

Possible cases:

BookingNotFoundException

Used when retrieving a booking that does not exist.

EventSessionNotFoundException

EventSession does not exist.

SeatHoldNotFoundException

A requested hold does not exist.

SeatHoldExpiredException

Hold has expired.

SeatHoldOwnershipException

Hold belongs to another user.

EventSessionNotAvailableException

Session cannot currently accept bookings.

SeatAlreadyBookedException

Seat has already been sold.

BookingConflictException

Database detects concurrent duplicate booking.

BookingAccessDeniedException

User attempts to access another user's booking.

Do not create all these exception classes if equivalent project exceptions already exist.

Reuse the existing exception mechanism where possible.

12. Acceptance Criteria
AC-01

Authenticated user can create a booking from their active SeatHolds.

AC-02

Unauthenticated user cannot create a booking.

AC-03

A booking must contain at least one BookingItem.

AC-04

User cannot book another user's SeatHold.

AC-05

Expired SeatHold cannot be converted into a Booking.

AC-06

Seat must belong to the EventSession's Venue.

AC-07

Already booked seats cannot be booked again.

AC-08

Backend calculates all ticket prices.

AC-09

Booking total equals the sum of BookingItem prices.

AC-10

Every Booking receives a unique booking code.

AC-11

Exactly one Payment is created for the Booking.

AC-12

Payment starts with PENDING.

AC-13

Payment amount equals Booking total.

AC-14

SeatHolds are removed after successful Booking creation.

AC-15

If any step fails, the entire transaction rolls back.

AC-16

User can retrieve their own bookings.

AC-17

User can retrieve their own booking detail.

AC-18

User cannot retrieve another user's booking.

AC-19

Booking history is never physically deleted.

AC-20

All API responses use ApiResponse<T>.

13. Coding Constraints

Follow:

PROJECT_CONTEXT.md
DATABASE.md
US-12

Rules:

Follow existing package structure.
Use constructor injection.
Use @RequiredArgsConstructor.
Do not use field injection.
Use ApiResponse<T>.
Use Jakarta Validation.
Keep Controller thin.
Put business logic in Service.
Keep Repository focused on data access.
Use @Transactional for booking creation.
Use BusinessException.
Do not throw raw RuntimeException.
Do not expose Entity directly through REST.
Use BigDecimal for monetary values.
Use LocalDateTime for timestamps.
Do not trust price/total from frontend.
Do not trust userId from frontend.
Do not create unnecessary tables.
Do not modify completed Flyway migrations.
Do not introduce payment gateway logic in US13.
Do not introduce Redis, Kafka, scheduler or distributed locks unless already required by the existing codebase.
Avoid N+1 queries.
Do not modify unrelated modules.

The current database specifically requires BigDecimal for monetary values, LAZY relationships and avoiding N+1 queries.

14. Review Checklist

Before considering US13 complete, verify:

Booking
 Booking belongs to current user.
 Booking belongs to exactly one EventSession.
 Booking contains at least one BookingItem.
 Booking code is unique.
 Booking total is calculated by backend.
Seat
 Every Seat belongs to EventSession Venue.
 Every SeatHold belongs to current user.
 Expired SeatHold is rejected.
 Already booked Seat is rejected.
 Duplicate seat booking is protected by DB constraint.
BookingItem
 One BookingItem represents one Seat.
 Price is calculated server-side.
 Price is stored as immutable booking price.
 event_session_id is consistent with Booking.
 (event_session_id, seat_id) uniqueness is respected.
Payment
 Exactly one Payment is created.
 Payment amount equals Booking total.
 Initial Payment status is PENDING.
 No payment gateway logic exists in US13.
SeatHold
 SeatHolds are validated before booking.
 Ownership is checked.
 Expiration is checked.
 Holds are removed after successful booking.
 Failed transaction does not partially remove holds.
Security
 Current user comes from SecurityContext.
 userId is not accepted as a trusted request field.
 Users cannot access another user's booking.
Performance
 No N+1 queries.
 Batch loading is used where appropriate.
 Repository queries are minimal.
15. Architecture Decisions
Decision 1 — Booking must be created from SeatHold

Decision

A user can only create BookingItems for seats currently held by that user.

Why

This creates a clear flow:

Seat Selection
     ↓
SeatHold
     ↓
Booking
     ↓
Payment

It prevents users from bypassing the temporary seat reservation mechanism.

Alternative

Allow Booking directly from Seat IDs.

Trade-off

Direct booking is simpler but introduces a larger concurrency problem and makes the purpose of US12 weaker.

Decision 2 — Backend calculates price

Decision

Backend calculates:

basePrice × priceMultiplier

and stores the final price in BookingItem.

Why

The frontend must never be trusted with financial values.

Alternative

Frontend sends calculated price.

Trade-off

Frontend calculation is simpler but insecure and allows price manipulation.

Decision 3 — BookingItem stores price snapshot

The final ticket price is stored in:

booking_item.price

This ensures historical bookings are not affected if Seat or EventSession pricing changes later.

The database explicitly defines BookingItem price as the final ticket price and describes it as immutable ticket information.

Decision 4 — Database protects duplicate seat sales

Application checks:

Is seat already booked?

are necessary for a clean business response.

However, the database constraint:

UNIQUE(event_session_id, seat_id)

is the final concurrency protection.

This provides defense in depth.

Decision 5 — Booking and Payment are created in one transaction

Creating:

Booking
BookingItems
Payment
SeatHold deletion

is one business operation.

Therefore they should either all succeed or all roll back.

16. Knowledge Check

After implementation, explain to the developer:

Java/Spring
How @Transactional works.
Why Controller should remain thin.
Why business rules belong in Service.
How Spring Data JPA repositories work.
Why DTOs are used instead of exposing entities.
Database
Why booking_item(event_session_id, seat_id) must be unique.
Why Seat belongs to Venue rather than EventSession.
Why BookingItem stores the ticket price.
Why Booking history should not be deleted.
Concurrency

Explain the difference between:

Application availability check

and:

Database unique constraint

and why both are required.

Security

Explain why:

currentUser

must come from Spring Security instead of:

{
  "userId": 123
}
Business Flow

The developer should understand:

Seat
 ↓
SeatHold
 ↓
Booking
 ↓
BookingItem
 ↓
Payment
 ↓
Payment Gateway

US13 ends at the creation of the PENDING Payment. Payment gateway processing belongs to the next payment-related feature.

17. Implementation Boundary

US13 should end at:

SeatHold
    ↓
Booking
    ↓
BookingItem
    ↓
Payment(PENDING)

It must not implement:

Payment Gateway
VNPay
PayOS
MOMO
Webhook
Payment Callback
Refund
QR scanning

Those should be implemented in the Payment/Ticket flow later.

Important note for AI

Before coding, AI must re-check the actual current codebase and current DATABASE.md, especially the final SeatHold lifecycle from US12. The current database explicitly requires expired holds not to block new bookings and defines (event_session_id, seat_id) as unique for SeatHold. If US12 implementation changed this strategy, US13 must follow the actual implemented strategy rather than inventing another one.

Before implementation, AI must inspect the actual SeatHold entity, repository and SeatHoldServiceImpl to determine whether one SeatHold represents one seat or a group of seats. CreateBookingRequest must follow the actual implemented model and must not infer the structure from documentation alone.