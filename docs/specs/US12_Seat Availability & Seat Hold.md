US12 — Seat Availability & Seat Hold
1. Objective

Implement seat availability and temporary seat holding for an EventSession.

The feature allows customers to:

View seats available for an EventSession.
View the current status of each seat.
Temporarily hold one or more seats.
Prevent multiple users from holding the same seat simultaneously.
Automatically consider expired holds as no longer active.
Prepare the selected seats for the Booking process.

The feature must use the existing:

Venue
Seat
EventSession
SeatHold
BookingItem

relationship.

Important Design Rule

Seat represents a physical seat belonging to a Venue.

A Seat does not belong directly to an EventSession.

Therefore:

EventSession
    ↓
Venue
    ↓
Seat

The availability of a Seat is calculated for a specific EventSession.

2. Scope
Included
Get seat map for an EventSession.
Get availability of seats for an EventSession.
Determine AVAILABLE, HELD, and BOOKED states.
Create temporary SeatHold.
Hold multiple seats in one request.
Validate EventSession.
Validate Seat ownership.
Prevent holding already booked seats.
Prevent holding seats held by another active hold.
Handle expired SeatHolds.
Release/cancel a user's SeatHold.
Validate hold duration.
Prepare held seats for Booking.
Not Included
Venue CRUD.
Seat CRUD.
Event CRUD.
EventSession CRUD.
Booking creation.
Payment.
Ticket generation.
QR code.
Promotion/Coupon.
Seat Template.
Seat Configuration.
Creating seats when creating an EventSession.
3. Business Flow
3.1 Get Seat Map
Client
  ↓
GET /api/v1/event-sessions/{sessionId}/seats
  ↓
Find EventSession
  ↓
Find all Seats belonging to EventSession's Venue
  ↓
Check BookingItem
  ↓
Check active SeatHold
  ↓
Determine seat status
  ↓
Return seat map

For each seat:

Booked
  → BOOKED

Active SeatHold exists
  → HELD

Otherwise
  → AVAILABLE
3.2 Hold Seats
Customer
  ↓
POST /api/v1/event-sessions/{sessionId}/seat-holds
  ↓
Validate request
  ↓
Validate authenticated user
  ↓
Find EventSession
  ↓
Validate EventSession status
  ↓
Validate requested seats
  ↓
Check seats belong to EventSession's Venue
  ↓
Check BookingItem
  ↓
Check active SeatHold
  ↓
Any seat unavailable?
  ├─ YES → Reject entire request
  └─ NO
       ↓
Create SeatHold
       ↓
Set expiration time
       ↓
Save
       ↓
Return hold information
Important

Holding seats must be atomic.

If the customer requests:

A01
A02
A03

and A02 is unavailable:

A01 → must NOT be held
A02 → unavailable
A03 → must NOT be held

The entire request must fail.

3.3 Release Seat Hold
Customer
  ↓
DELETE /api/v1/seat-holds/{id}
  ↓
Find SeatHold
  ↓
Check owner
  ↓
Check current status
  ↓
Release hold
  ↓
Return success

A customer must not be able to release another customer's hold.

3.4 Expired Seat Hold

A SeatHold has an expiration time.

When:

expiresAt <= currentTime

the hold is considered expired.

An expired hold must no longer block another customer from selecting the seat.

The system does not necessarily need a scheduled job for the initial implementation.

Availability checks can treat expired holds as inactive.

4. Functional Requirements
FR-01

Allow customers/public clients to retrieve the seat map of an EventSession.

FR-02

Return the availability status of each Seat for the selected EventSession.

FR-03

A Seat must belong to the Venue of the EventSession.

FR-04

A booked Seat must be returned as:

BOOKED
FR-05

A Seat with an active SeatHold must be returned as:

HELD
FR-06

A Seat without an active hold or booking must be returned as:

AVAILABLE
FR-07

Authenticated customers can temporarily hold available Seats.

FR-08

One hold request may contain multiple Seats.

FR-09

The system must reject the entire hold request if at least one requested Seat is unavailable.

FR-10

A SeatHold must belong to exactly one User.

FR-11

A SeatHold must be associated with the relevant EventSession and Seat according to the existing database design.

FR-12

A SeatHold must contain an expiration time.

FR-13

Expired SeatHolds must not block Seat availability.

FR-14

Only the owner of a SeatHold can release it.

FR-15

A Seat cannot be held if it has already been booked for the EventSession.

FR-16

A Seat cannot be held if another active SeatHold already exists for that EventSession.

FR-17

All write operations must use the existing ApiResponse<T> structure.

5. Business Rules
BR-01 — Seat Ownership

A Seat is physically owned by a Venue.

Venue 1 ─── * Seat

A Seat is available for an EventSession only when:

seat.venue_id == event_session.venue_id
BR-02 — Seat Availability

For a given EventSession:

AVAILABLE
=
no active SeatHold
AND
no BookingItem
HELD
=
active SeatHold exists
AND
no BookingItem
BOOKED
=
valid BookingItem exists
BR-03 — Expired Hold

A SeatHold is active only when:

expires_at > current_time

Therefore:

expires_at <= current_time

means the hold is expired.

BR-04 — Hold Duration

The hold duration must use the project's configured value.

Do not hard-code different hold durations in different services.

If the project does not yet define a constant, create one shared configuration/constant rather than scattering magic numbers.

BR-05 — Ownership

A customer can release only their own SeatHold.

seatHold.user_id == authenticatedUser.id

Otherwise reject the request.

BR-06 — Atomic Hold

A multi-seat hold operation must be all-or-nothing.

Example:

Request:
A01
A02
A03

If:

A02 = BOOKED

then:

A01 = not held
A02 = BOOKED
A03 = not held
BR-07 — No Double Hold

A Seat cannot have two active holds for the same EventSession.

BR-08 — No Hold on Booked Seat

A booked Seat cannot be held again.

BR-09 — Session Validation

Seat selection must only be allowed for a valid EventSession.

At minimum:

EventSession exists
AND
EventSession is not CANCELLED
AND
EventSession is not FINISHED

Additional status restrictions must follow the existing EventSession status design.

BR-10 — Venue Consistency

If the EventSession belongs to:

Venue A

and the requested Seat belongs to:

Venue B

the request must be rejected.

BR-11 — Booking Dependency

SeatHold exists to temporarily reserve seats before Booking.

The intended flow is:

Seat Selection
      ↓
SeatHold
      ↓
Booking
      ↓
BookingItem

US12 does not create Booking or BookingItem.

BR-12 — Concurrency

Two customers may attempt to hold the same Seat at nearly the same time.

The implementation must prevent both requests from successfully holding the same Seat.

Use appropriate transactional/database protection rather than relying only on:

if available
then save

because two requests can pass the availability check simultaneously.

6. API Contract
6.1 Get Seat Map
Endpoint
GET /api/v1/event-sessions/{sessionId}/seats
Response
{
  "success": true,
  "message": "Seats retrieved successfully.",
  "data": [
    {
      "id": 1,
      "seatNumber": "A01",
      "rowLabel": "A",
      "seatType": "STANDARD",
      "price": 120000,
      "status": "AVAILABLE"
    },
    {
      "id": 2,
      "seatNumber": "A02",
      "rowLabel": "A",
      "seatType": "STANDARD",
      "price": 120000,
      "status": "BOOKED"
    },
    {
      "id": 3,
      "seatNumber": "A03",
      "rowLabel": "A",
      "seatType": "STANDARD",
      "price": 120000,
      "status": "HELD"
    }
  ],
  "timestamp": "2026-08-08T12:00:00"
}

Do not expose internal SeatHold or BookingItem implementation details in the public seat response.

6.2 Hold Seats
Endpoint
POST /api/v1/event-sessions/{sessionId}/seat-holds
Request
{
  "seatIds": [1, 2, 3]
}
Response
{
  "success": true,
  "message": "Seats held successfully.",
  "data": {
    "holdId": 100,
    "eventSessionId": 10,
    "seatIds": [1, 2, 3],
    "expiresAt": "2026-08-08T12:10:00"
  },
  "timestamp": "2026-08-08T12:00:00"
}
6.3 Release Seat Hold
Endpoint
DELETE /api/v1/seat-holds/{holdId}
Response
{
  "success": true,
  "message": "Seat hold released successfully.",
  "data": null,
  "timestamp": "2026-08-08T12:05:00"
}
6.4 Get My Active Hold

If the existing project requires the frontend to recover a user's current hold:

GET /api/v1/seat-holds/me
Response
{
  "success": true,
  "message": "Active seat hold retrieved successfully.",
  "data": {
    "holdId": 100,
    "eventSessionId": 10,
    "seatIds": [1, 2, 3],
    "expiresAt": "2026-08-08T12:10:00"
  },
  "timestamp": "2026-08-08T12:05:00"
}

This endpoint is optional if the current frontend flow does not need it.

# 7. Validation Rules

## 7.1 HoldSeatsRequest

### seatIds

```java
@NotEmpty
```

Rules:

* Must contain at least one Seat ID.
* Must not contain duplicate Seat IDs.
* The number of requested seats must not exceed the project-defined maximum, if one exists.

Example:

```json
{
  "seatIds": [1, 2, 3]
}
```

Invalid:

```json
{
  "seatIds": []
}
```

Invalid:

```json
{
  "seatIds": [1, 1, 2]
}
```

---

## 7.2 EventSession ID

Must:

* Not be null.
* Reference an existing EventSession.
* Not belong to a cancelled session.
* Not belong to a finished session.

---

## 7.3 Seat IDs

For every requested Seat:

```text
Seat must exist
```

Then:

```text
seat.venue_id == eventSession.venue_id
```

If the Seat belongs to another Venue:

```text
VENUE_SEAT_MISMATCH
```

---

## 7.4 Seat Availability

Each requested Seat must satisfy:

```text
NOT BOOKED
AND
NOT actively HELD by another user
```

Expired SeatHolds must not be considered active.

---

## 7.5 Existing User Hold

If the business rule allows only one active hold per user/session, reject a second active hold for the same EventSession.

Do not create duplicate active holds for the same user and session unless the existing database/business design explicitly supports it.

---

## 7.6 Release Hold

Before releasing a SeatHold:

```text
hold exists
AND
hold.user_id == authenticatedUser.id
```

Otherwise reject the request.

---

# 8. Database Impact

US12 uses the existing database tables.

## Read

```text
event_session
venue
seat
seat_hold
booking_item
```

Depending on the existing entity relationships, `user` may also be loaded when validating SeatHold ownership.

---

## Insert

```text
seat_hold
```

When a customer successfully holds seats.

---

## Update

Only update `seat_hold` if the existing schema/design requires a release status or release timestamp.

Do not invent additional columns if they do not exist in `DATABASE.md`.

---

## Delete

If the current `SeatHold` design represents a temporary record that is removed when released:

```text
DELETE seat_hold
```

If the existing schema uses a status-based lifecycle, follow that schema instead.

**AI must inspect `DATABASE.md` and the existing `SeatHold` entity before deciding between physical deletion and status update.**

---

## Important Database Rule

Do NOT modify:

```text
seat
```

when a customer holds a seat.

The physical Seat remains unchanged.

The hold belongs to the relationship between:

```text
User
EventSession
Seat
```

through `SeatHold`.

---

# 9. Expected Classes

## Controller

```text
SeatAvailabilityController
```

or, if the existing project groups the functionality differently:

```text
SeatHoldController
```

Prefer the existing project naming convention.

Endpoints:

```text
GET  /api/v1/event-sessions/{sessionId}/seats
POST /api/v1/event-sessions/{sessionId}/seat-holds
DELETE /api/v1/seat-holds/{holdId}
GET /api/v1/seat-holds/me
```

---

## Service

```text
SeatHoldService
SeatHoldServiceImpl
```

Responsibilities:

* Validate EventSession.
* Load requested Seats.
* Validate Venue ownership.
* Check BookingItem.
* Check active SeatHold.
* Create SeatHold.
* Calculate expiration.
* Release SeatHold.
* Validate ownership.
* Handle concurrency safely.

The Controller must not contain these business rules.

---

## DTO

```text
HoldSeatsRequest
SeatAvailabilityResponse
SeatAvailabilityItem
SeatHoldResponse
```

If the existing project uses different DTO naming conventions, follow those conventions.

---

## Repository

Existing repositories should be reused where possible:

```text
EventSessionRepository
SeatRepository
SeatHoldRepository
BookingItemRepository
```

Do not create duplicate repositories.

---

## Suggested Repository Operations

### SeatRepository

```text
findAllByVenueId(venueId)
findAllById(...)
```

---

### BookingItemRepository

Need a query capable of checking whether a Seat is already booked for an EventSession.

Conceptually:

```text
existsBySeatIdAndEventSessionId(...)
```

The exact method name depends on the current entity relationship.

---

### SeatHoldRepository

Need queries capable of:

```text
find active hold for seat + event session
```

and:

```text
find active holds for user + event session
```

Expired holds must not be considered active.

Conceptually:

```text
expires_at > current_time
```

---

# 10. Transaction

## Get Seat Map

Read-only operation.

Prefer:

```java
@Transactional(readOnly = true)
```

if consistent with the existing project.

---

## Hold Seats

Must use:

```java
@Transactional
```

because the operation performs multiple database operations that must behave atomically.

Flow:

```text
Validate
  ↓
Check availability
  ↓
Create SeatHold
  ↓
Commit
```

If any requested Seat becomes unavailable:

```text
ROLLBACK
```

No partial hold should remain.

---

## Release Hold

Use:

```java
@Transactional
```

because the SeatHold is modified/deleted.

---

## Concurrency

The most important part of US12 is concurrent requests.

Example:

```text
User A                  User B

Check A01 available     Check A01 available
        ↓                       ↓
Create hold A01         Create hold A01
```

Without database protection, both requests could succeed.

The implementation must use an appropriate locking/constraint strategy supported by the existing database and entity design.

Do not solve concurrency only with Java:

```java
if (available) {
    save();
}
```

because this is vulnerable to race conditions.

The AI must inspect the current `SeatHold` schema before choosing:

* pessimistic locking,
* unique database constraint,
* appropriate transaction isolation,
* or a combination.

Do not introduce a new mechanism without checking the existing database design.

---

# 11. Exception Cases

Use the project's existing `BusinessException` mechanism.

Possible business errors:

### EVENT_SESSION_NOT_FOUND

EventSession does not exist.

---

### EVENT_SESSION_NOT_AVAILABLE

The EventSession cannot currently be used for seat selection.

---

### SEAT_NOT_FOUND

One or more requested Seats do not exist.

---

### SEAT_VENUE_MISMATCH

Requested Seat does not belong to the EventSession's Venue.

---

### SEAT_ALREADY_BOOKED

At least one requested Seat has already been booked.

---

### SEAT_ALREADY_HELD

At least one requested Seat is actively held by another user.

---

### SEAT_HOLD_NOT_FOUND

Requested SeatHold does not exist.

---

### SEAT_HOLD_NOT_OWNER

Authenticated user does not own the SeatHold.

---

### SEAT_HOLD_EXPIRED

The requested SeatHold has expired.

---

### DUPLICATE_SEAT_ID

The request contains the same Seat ID more than once.

---

### INVALID_SEAT_HOLD

The SeatHold request violates the project's business rules.

---

### SEAT_HOLD_CONFLICT

Another request successfully acquired the requested Seat during the current transaction.

This should be handled as a business-level error rather than exposing a raw database exception.

---

# 12. Acceptance Criteria

## Seat Map

* [ ] Client can retrieve the Seat map for an EventSession.
* [ ] Only Seats belonging to the EventSession's Venue are returned.
* [ ] Booked Seats are returned as `BOOKED`.
* [ ] Actively held Seats are returned as `HELD`.
* [ ] Other Seats are returned as `AVAILABLE`.
* [ ] Expired holds do not make a Seat `HELD`.

## Seat Hold

* [ ] Authenticated customer can hold available Seats.
* [ ] Multiple Seats can be held in one request.
* [ ] Duplicate Seat IDs are rejected.
* [ ] Seats belonging to another Venue are rejected.
* [ ] Booked Seats are rejected.
* [ ] Seats actively held by another user are rejected.
* [ ] The entire request is atomic.
* [ ] Successful holds receive an expiration time.
* [ ] Expired holds no longer block availability.

## Release

* [ ] Customer can release their own SeatHold.
* [ ] Customer cannot release another user's SeatHold.
* [ ] Expired SeatHolds are handled correctly.
* [ ] Releasing a hold makes the Seats available again.

## Concurrency

* [ ] Two users cannot successfully hold the same Seat simultaneously.
* [ ] Database/transaction protection is used where required.
* [ ] No partial SeatHold is created when a multi-seat request fails.

---

# 13. Coding Constraints

* Follow `PROJECT_CONTEXT.md`.
* Follow `DATABASE.md`.
* Follow the existing `SeatHold` entity and schema.
* Do not introduce `SeatTemplate`.
* Do not introduce `SeatConfiguration`.
* Do not introduce `VenueSeat`.
* Do not create Seats during SeatHold.
* Do not modify the physical Seat record when holding/releasing a Seat.
* Use constructor injection.
* Do not use field injection.
* Use Jakarta Validation.
* Use `ApiResponse<T>`.
* Use existing `BusinessException`.
* Keep Controller thin.
* Keep business logic inside Service.
* Reuse existing repositories.
* Do not duplicate existing authentication/security logic.
* Do not modify completed Flyway migrations.
* Do not introduce unnecessary abstractions.
* Do not expose internal database details in API responses.
* Do not return raw JPA entities from the Controller.
* Use DTOs for API responses.
* Use transactions for hold/release operations.
* Handle concurrency explicitly.
* Follow existing naming conventions.

---

# 14. Review Checklist

Before completing US12:

### Architecture

* [ ] Controller contains only HTTP/API logic.
* [ ] Service contains business logic.
* [ ] Repository contains database access.
* [ ] DTOs are used.
* [ ] Existing exception handling is reused.

### Seat Ownership

* [ ] Seat belongs to Venue.
* [ ] EventSession belongs to Venue.
* [ ] Requested Seat belongs to EventSession's Venue.
* [ ] No Seat is copied to EventSession.

### Availability

* [ ] BookingItem is checked.
* [ ] Active SeatHold is checked.
* [ ] Expired SeatHold is ignored.
* [ ] Status is calculated per EventSession.

### Hold

* [ ] Multiple Seats supported.
* [ ] Request is atomic.
* [ ] Expiration time is stored.
* [ ] Owner is validated on release.

### Concurrency

* [ ] Race condition is considered.
* [ ] Database/transaction protection is implemented.
* [ ] Same Seat cannot be successfully held by two users.

### Security

* [ ] Seat map follows the project's public/private security policy.
* [ ] Hold requires authentication.
* [ ] Release requires authentication.
* [ ] User can only release their own hold.

### Database

* [ ] No unnecessary schema changes.
* [ ] No completed Flyway migration modified.
* [ ] No Seat status updated during hold.
* [ ] Existing SeatHold structure is respected.

---

# 15. Architecture Decisions

## Decision 1 — Seat status is not stored in Seat

### Why

A physical Seat can have different states for different EventSessions.

Example:

```text
Session A → A01 BOOKED
Session B → A01 AVAILABLE
```

Therefore status cannot belong to the physical Seat.

### Alternative

Add:

```text
status
```

to the `seat` table.

### Trade-off

That would make the status global and incorrect for multiple sessions.

---

## Decision 2 — Use SeatHold for temporary reservation

### Why

Customers need time to complete the Booking process.

SeatHold provides temporary ownership before Booking.

### Alternative

Immediately create Booking when the customer selects a Seat.

### Trade-off

This would create incomplete bookings and make abandoned selections difficult to manage.

---

## Decision 3 — Expired SeatHold is treated as inactive

### Why

A temporary hold should not permanently block a Seat.

### Alternative

Run a scheduled background job to physically delete expired holds.

### Trade-off

A scheduled cleanup job can improve database cleanliness, but the initial implementation does not need to depend on a background scheduler.

Availability logic should still check:

```text
expiresAt > now
```

---

## Decision 4 — Multi-seat hold is atomic

### Why

A customer expects all selected Seats to be held together.

If one Seat is unavailable, creating only part of the requested hold creates an inconsistent user experience.

### Alternative

Allow partial success.

### Trade-off

Partial success complicates the frontend and can cause unexpected seat selections.

---

## Decision 5 — Concurrency must be handled at database/transaction level

### Why

Two HTTP requests can execute the availability check at the same time.

Application-level checking alone is insufficient.

### Alternative

Only perform a normal `exists` check before insert.

### Trade-off

The simple approach is easier but vulnerable to race conditions.

For a real ticket booking system, concurrency protection is essential.

---

# 16. Knowledge Check

After implementation, explain the following to the developer:

### Spring Features

* `@Transactional`
* Spring Data JPA
* Jakarta Validation
* Spring Security
* Constructor Injection
* `@RequiredArgsConstructor`

### Important Concepts

1. Why does Seat belong to Venue rather than EventSession?
2. Why can't Seat status be stored directly in the Seat table?
3. How is `AVAILABLE` determined?
4. How is `HELD` determined?
5. How is `BOOKED` determined?
6. Why do expired SeatHolds stop blocking a Seat?
7. Why must multi-seat holding be atomic?
8. Why can a normal `existsBy...()` check still have a race condition?
9. How does a database constraint or lock prevent double holding?
10. Why does SeatHold belong to the authenticated user?

### Interview Questions

Possible interview questions:

1. How would you prevent two users from booking the same seat?
2. What is the difference between a Seat and a SeatHold?
3. Why is `@Transactional` important in seat reservation?
4. What happens when a SeatHold expires?
5. How would you implement a 10-minute seat reservation?
6. What happens if two users click the same seat at exactly the same time?
7. Would you use optimistic or pessimistic locking here? Why?
8. Why shouldn't you update `seat.status` when a customer selects a seat?
9. How would you clean up expired SeatHolds in production?
10. How would Redis distributed locking change the architecture at larger scale?

### Most Important Code to Understand

The developer should understand these parts first:

```text
1. Seat map query
2. Seat availability calculation
3. Active SeatHold detection
4. BookingItem detection
5. Multi-seat atomic transaction
6. Seat ownership validation
7. Expiration handling
8. Concurrency protection
```

**US12 is complete when these rules work together without allowing two customers to successfully reserve the same Seat for the same EventSession.**
