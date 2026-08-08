# US-11 — Event Session Management

## 1. Objective

Implement Event Session Management.

An EventSession represents one scheduled showing of an Event at a specific Venue.

Example:

Event:
"Avengers: Secret Wars"

Sessions:

10:00 - CGV Vincom
14:00 - CGV Vincom
19:00 - CGV Vincom

The EventSession uses the physical Seats already belonging to its Venue.

IMPORTANT:

- Seat belongs to Venue.
- Seat does NOT belong to EventSession.
- Creating an EventSession must NOT create new Seat records.
- Seat availability is determined by SeatHold and BookingItem.

---

# 2. Scope

## Included

- Get Event Sessions
- Get Event Session by ID
- Create Event Session
- Update Event Session
- Cancel Event Session
- Validate Event
- Validate Venue
- Validate session time
- Validate booking time
- Prevent invalid schedules
- Prevent conflicting sessions at the same Venue
- Return session information
- Reuse existing Venue Seats

## Not Included

- Event Management
- Venue Management
- Seat CRUD
- Seat Template
- Seat Configuration
- Booking
- Payment
- Seat Hold API
- Ticket generation
- QR code generation
- Seat creation when creating a session

---

# 3. Business Flow

## 3.1 Get Event Sessions

Client
  ↓
GET /api/v1/event-sessions
  ↓
Validate query parameters
  ↓
Load Event Sessions
  ↓
Return session list

---

## 3.2 Get Event Session By ID

Client
  ↓
GET /api/v1/event-sessions/{id}
  ↓
Find EventSession
  ↓
Exists?
  ├─ NO → BusinessException
  └─ YES
       ↓
Return session detail

---

## 3.3 Create Event Session

Admin
  ↓
POST /api/v1/admin/event-sessions
  ↓
Validate request
  ↓
Find Event
  ↓
Find Venue
  ↓
Validate Event status
  ↓
Validate Venue
  ↓
Validate time range
  ↓
Validate booking window
  ↓
Check conflicting session
  ↓
Create EventSession
  ↓
Save EventSession
  ↓
Reuse existing Venue Seats
  ↓
Return created session

IMPORTANT:

No Seat records are created during this process.

---

## 3.4 Update Event Session

Admin
  ↓
PUT /api/v1/admin/event-sessions/{id}
  ↓
Validate request
  ↓
Find EventSession
  ↓
Validate Event
  ↓
Validate Venue
  ↓
Validate schedule
  ↓
Check session conflict
  ↓
Update fields
  ↓
Save
  ↓
Return updated session

---

## 3.5 Cancel Event Session

Admin
  ↓
PATCH /api/v1/admin/event-sessions/{id}/cancel
  ↓
Find EventSession
  ↓
Check current status
  ↓
Change status to CANCELLED
  ↓
Save
  ↓
Return success

Do NOT physically delete the EventSession.

Historical Booking data must remain.

---

# 4. Functional Requirements

### FR-01

Allow clients to retrieve Event Sessions.

### FR-02

Allow clients to retrieve an Event Session by ID.

### FR-03

Allow ADMIN to create an Event Session.

### FR-04

Allow ADMIN to update an Event Session.

### FR-05

Allow ADMIN to cancel an Event Session.

### FR-06

An EventSession must belong to exactly one Event.

### FR-07

An EventSession must belong to exactly one Venue.

### FR-08

Validate that the Event exists before creating or updating a session.

### FR-09

Validate that the Venue exists before creating or updating a session.

### FR-10

Validate that start_time is before end_time.

### FR-11

Validate booking_open and booking_close.

### FR-12

Prevent conflicting sessions at the same Venue.

### FR-13

Do not create or copy Seat records when creating an EventSession.

### FR-14

EventSession uses the Seats already belonging to its Venue.

### FR-15

Return all API responses using ApiResponse<T>.

---

# 5. Business Rules

### BR-01 — Event

Every EventSession must belong to an existing Event.

### BR-02 — Venue

Every EventSession must belong to an existing Venue.

### BR-03 — Seat Ownership

Seats available for a session are the physical Seats belonging to:

event_session.venue_id

The system must never attach a Seat from another Venue to the session.

### BR-04 — No Seat Duplication

Creating an EventSession must NOT create new Seat records.

Example:

Venue A already has:

A01
A02
A03

Creating a new EventSession at Venue A continues to use:

A01
A02
A03

### BR-05 — Time Range

```text
start_time < end_time
BR-06 — Booking Window
booking_open < booking_close

The booking window must finish before or at the session start:

booking_close <= start_time
BR-07 — Positive Session Duration

A session cannot have zero or negative duration.

BR-08 — Session Conflict

Two sessions must not overlap at the same Venue.

For sessions A and B at the same Venue:

A.start_time < B.end_time
AND
B.start_time < A.end_time

means the sessions overlap and must be rejected.

Sessions at different Venues may have the same time.

BR-09 — Status

Use:

UPCOMING
BOOKING_OPEN
BOOKING_CLOSED
FINISHED
CANCELLED

Do not invent another status.

BR-10 — Cancellation

Cancelling a session must update its status to:

CANCELLED

Do not physically delete the EventSession.

BR-11 — Booking History

Existing Booking and BookingItem records must not be deleted when an EventSession is cancelled.

BR-12 — Seat Status

Seat does NOT contain:

AVAILABLE
RESERVED
BOOKED

Seat availability is determined per EventSession using:

SeatHold
BookingItem
BR-13 — Availability

For a specific EventSession:

AVAILABLE
=
no active SeatHold
AND
no valid BookingItem
RESERVED
=
active SeatHold exists
BOOKED
=
valid BookingItem exists

This logic belongs to the booking/seat-selection feature, not EventSession CRUD.

6. API Contract
6.1 Get All Event Sessions
Endpoint
GET /api/v1/event-sessions
Optional Query Parameters
eventId
venueId
status

Pagination should only be added if the existing project convention already supports it.

Response
{
  "success": true,
  "message": "Event sessions retrieved successfully.",
  "data": [
    {
      "id": 1,
      "eventId": 10,
      "venueId": 5,
      "startTime": "2026-08-20T19:00:00",
      "endTime": "2026-08-20T21:30:00",
      "bookingOpen": "2026-08-10T00:00:00",
      "bookingClose": "2026-08-20T18:30:00",
      "basePrice": 120000,
      "status": "UPCOMING"
    }
  ],
  "timestamp": "2026-08-08T10:00:00"
}
6.2 Get Event Session By ID
Endpoint
GET /api/v1/event-sessions/{id}
Response
{
  "success": true,
  "message": "Event session retrieved successfully.",
  "data": {
    "id": 1,
    "eventId": 10,
    "venueId": 5,
    "startTime": "2026-08-20T19:00:00",
    "endTime": "2026-08-20T21:30:00",
    "bookingOpen": "2026-08-10T00:00:00",
    "bookingClose": "2026-08-20T18:30:00",
    "basePrice": 120000,
    "status": "UPCOMING"
  },
  "timestamp": "2026-08-08T10:00:00"
}
6.3 Create Event Session
Endpoint
POST /api/v1/admin/event-sessions
Request
{
  "eventId": 10,
  "venueId": 5,
  "startTime": "2026-08-20T19:00:00",
  "endTime": "2026-08-20T21:30:00",
  "bookingOpen": "2026-08-10T00:00:00",
  "bookingClose": "2026-08-20T18:30:00",
  "basePrice": 120000
}
Response
{
  "success": true,
  "message": "Event session created successfully.",
  "data": {
    "id": 1,
    "eventId": 10,
    "venueId": 5,
    "startTime": "2026-08-20T19:00:00",
    "endTime": "2026-08-20T21:30:00",
    "bookingOpen": "2026-08-10T00:00:00",
    "bookingClose": "2026-08-20T18:30:00",
    "basePrice": 120000,
    "status": "UPCOMING"
  },
  "timestamp": "2026-08-08T10:00:00"
}
6.4 Update Event Session
Endpoint
PUT /api/v1/admin/event-sessions/{id}
Request
{
  "eventId": 10,
  "venueId": 5,
  "startTime": "2026-08-20T20:00:00",
  "endTime": "2026-08-20T22:30:00",
  "bookingOpen": "2026-08-10T00:00:00",
  "bookingClose": "2026-08-20T19:30:00",
  "basePrice": 130000
}
Response

Use the same structure as Create Event Session.

6.5 Cancel Event Session
Endpoint
PATCH /api/v1/admin/event-sessions/{id}/cancel
Response
{
  "success": true,
  "message": "Event session cancelled successfully.",
  "data": null,
  "timestamp": "2026-08-08T10:00:00"
}
7. Validation Rules
CreateEventSessionRequest
eventId
@NotNull

Must reference an existing Event.

venueId
@NotNull

Must reference an existing Venue.

startTime
@NotNull

Must be before endTime.

endTime
@NotNull

Must be after startTime.

bookingOpen
@NotNull

Must be before bookingClose.

bookingClose
@NotNull

Must satisfy:

bookingClose <= startTime
basePrice
@NotNull
@DecimalMin("0.01")

Must be greater than zero.

Do not put cross-field validation logic inside the Controller.

Cross-field business validation belongs in the Service.

8. Database Impact
Read
event
venue
event_session
Insert
event_session
Update
event_session
Delete

None.

Cancellation is implemented by updating:

event_session.status = CANCELLED
Important

Do NOT insert into:

seat

when creating EventSession.

Seats already belong to Venue.

9. Repository Requirements
EventSessionRepository

Required operations:

findById(id)

findAll(...)

existsByEventIdAndVenueIdAndStartTime(...)

existsOverlappingSession(...)

The exact method name may differ depending on the implementation.

The repository must support checking whether another session overlaps at the same Venue.

Conceptually:

venue_id = requestedVenue
AND
start_time < requestedEndTime
AND
end_time > requestedStartTime

When updating a session, exclude the current session ID from the conflict check.

EventRepository

Use existing:

findById(...)

No new repository abstraction is required.

VenueRepository

Use existing:

findById(...)

No new repository abstraction is required.

10. Expected Classes
Entity
EventSession

Use the existing database structure.

Fields:

id
startTime
endTime
bookingOpen
bookingClose
basePrice
status
event
venue
createdAt
updatedAt
Controller
EventSessionController
Service
EventSessionService
EventSessionServiceImpl
DTO
CreateEventSessionRequest
UpdateEventSessionRequest
EventSessionResponse
Mapper
EventSessionMapper
Repository
EventSessionRepository

Reuse:

EventRepository
VenueRepository
11. Transaction
Create

Use:

@Transactional

because EventSession creation is a database write operation.

Update

Use:

@Transactional
Cancel

Use:

@Transactional

because the session status is updated.

Read

Read operations do not require a write transaction unless required by the existing implementation.

12. Exception Cases

Use existing BusinessException / project-specific exceptions.

Possible cases:

EVENT_NOT_FOUND

Event does not exist.

VENUE_NOT_FOUND

Venue does not exist.

EVENT_SESSION_NOT_FOUND

EventSession does not exist.

INVALID_SESSION_TIME
startTime >= endTime
INVALID_BOOKING_WINDOW
bookingOpen >= bookingClose
BOOKING_CLOSE_AFTER_SESSION_START
bookingClose > startTime
EVENT_SESSION_CONFLICT

Another EventSession overlaps at the same Venue.

SESSION_ALREADY_CANCELLED

Cannot cancel an already cancelled session.

SESSION_ALREADY_FINISHED

Do not modify/cancel a finished session unless explicitly supported.

13. Acceptance Criteria
Admin can create an EventSession.
Admin can update an EventSession.
Admin can cancel an EventSession.
Client can retrieve EventSessions.
Client can retrieve an EventSession by ID.
Event must exist.
Venue must exist.
startTime must be before endTime.
bookingOpen must be before bookingClose.
bookingClose must not be after startTime.
Overlapping sessions at the same Venue are rejected.
Sessions at different Venues may have the same schedule.
EventSession does not create Seat records.
EventSession uses Seats belonging to its Venue.
Cancelled sessions are not physically deleted.
Existing Booking history is preserved.
Admin endpoints require ADMIN role.
Responses use ApiResponse<T>.
Validation errors are handled globally.
14. Coding Constraints
Follow PROJECT_CONTEXT.md.
Follow DATABASE.md.
Follow existing package structure.
Use constructor injection.
Use @RequiredArgsConstructor where appropriate.
Do not use field injection.
Use Jakarta Validation.
Use ApiResponse<T>.
Use BusinessException.
Keep Controller thin.
Put business logic in Service.
Prefer derived repository queries.
Do not introduce SeatTemplate.
Do not introduce VenueSeat.
Do not introduce SeatConfiguration.
Do not create Seat records when creating EventSession.
Do not modify completed Flyway migrations.
Do not modify unrelated modules.
Do not introduce unnecessary abstractions.
Keep implementation production-ready and easy to understand.
15. Review Checklist

Before completing US11, verify:

Event
 Event exists.
 Event status is valid for creating a session.
Venue
 Venue exists.
 Venue is valid for hosting the session.
Schedule
 startTime < endTime.
 bookingOpen < bookingClose.
 bookingClose <= startTime.
 No overlapping session exists at the same Venue.
Seat
 No Seat is created when creating a session.
 Existing Venue Seats are reused.
 Seat availability is not stored in Seat.
Security
 Create requires ADMIN.
 Update requires ADMIN.
 Cancel requires ADMIN.
API
 Controller is thin.
 Response uses ApiResponse<T>.
 DTOs are used.
 Validation uses Jakarta Validation.
Database
 Only event_session is written during session creation.
 Existing Flyway migrations are not modified.
 Event/Booking history is preserved.
16. Architecture Decisions
Decision 1 — Seat belongs to Venue
Why

A Seat represents a physical seat inside a real Venue.

The same physical seat can be used by multiple EventSessions.

Alternative

Create new Seat records for every EventSession.

Trade-off

The alternative causes unnecessary duplication and makes seat management difficult.

The current design keeps physical seat data centralized in Venue.

Decision 2 — Do not create Seat when creating EventSession
Why

EventSession only represents a schedule.

The Venue already owns its physical Seats.

Alternative

Copy Venue Seats into EventSession.

Trade-off

Copying seats would duplicate data and make seat updates difficult.

Therefore EventSession references Venue and reuses its Seats.

Decision 3 — Seat availability is session-specific
Why

The same Seat can be:

BOOKED

in one EventSession and:

AVAILABLE

in another.

Alternative

Store status directly in Seat.

Trade-off

That would produce incorrect data because Seat is shared by multiple sessions.

Therefore availability is calculated using:

EventSession
+
Seat
+
SeatHold
+
BookingItem
Decision 4 — Cancel instead of delete
Why

EventSession may already have Booking history.

Deleting the session could break historical business data.

Alternative

Physically delete the session.

Trade-off

Physical deletion is simpler but unsafe for a ticket booking system.

Therefore cancellation changes:

status = CANCELLED
Decision 5 — Prevent overlapping sessions at the same Venue
Why

A Venue cannot normally host two events in the same time period.

Alternative

Allow overlapping sessions and let administrators handle conflicts manually.

Trade-off

That creates invalid schedules and poor user experience.

The backend should reject conflicts before saving.

17. Knowledge Check

After implementation, explain:

Spring Features
Spring MVC
Spring Data JPA
Jakarta Validation
Spring Security
@Transactional
Lombok constructor injection
Design Patterns
Service Layer
Repository Pattern
DTO Pattern
Mapper Pattern
Guard Clause
Important Concepts

Explain:

Why Seat belongs to Venue.
Why EventSession does not create Seats.
How seat availability is calculated.
How overlapping sessions are detected.
Why EventSession is cancelled instead of deleted.
Why business validation belongs in Service.
Why database constraints are still required in addition to Service validation.
Interview Questions

Possible questions:

Why don't you store seat status directly in Seat?
How can the same seat be booked for different sessions?
How do you prevent two sessions from using the same Venue at the same time?
How do you prevent double booking of a Seat?
Why do you use @Transactional?
Why should EventSession not create Seat records?
What happens if two requests create overlapping sessions simultaneously?
Why should historical EventSession records not be deleted?