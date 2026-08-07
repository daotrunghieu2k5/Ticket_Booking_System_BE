# US10 – Event Management

---

# 1. Objective

Implement Event Management for the Ticket Booking System.

This module allows administrators to manage event information that will later be used for event sessions, seat booking, and online ticket sales.

An Event represents the core business object (e.g., concert, movie, theater performance, sports match).

It contains general information only.

Scheduling, venue assignment, and seat allocation are handled in later user stories.

This module belongs to the Event Management domain.

---

# 2. Scope

## Included

### Public Features

- Get event list
- View event detail
- Search events by keyword
- Filter events by category
- Filter events by status (if applicable)
- Pagination for event list

### Admin Features

- Create event
- Update event
- Delete event
- Activate / deactivate event
- Validate referenced category
- Validate event information

---

## Not Included

The following features belong to later User Stories.

- Event Session Management
- Seat Management
- Ticket Pricing
- Booking
- Payment
- Seat Reservation
- QR Ticket
- Event Review
- Favorite Events
- Event Recommendation
- Promotion
- Coupon

---

# 3. Business Flow

## 3.1 Public - Get Event List

```text
Client
    ↓
GET /api/v1/events
    ↓
Load events
    ↓
Apply filters
    ↓
Apply pagination
    ↓
Return event list
```

---

## 3.2 Public - Get Event Detail

```text
Client
    ↓
GET /api/v1/events/{id}
    ↓
Find event
    ↓
Event exists?
    ├── NO → throw BusinessException
    └── YES
            ↓
Return event detail
```

---

## 3.3 Admin - Create Event

```text
Admin
    ↓
POST /api/v1/admin/events
    ↓
Validate request
    ↓
Validate category
    ↓
Category exists?
    ├── NO → throw BusinessException
    └── YES
            ↓
Create Event
            ↓
Save Event
            ↓
Return created event
```

---

## 3.4 Admin - Update Event

```text
Admin
    ↓
PUT /api/v1/admin/events/{id}
    ↓
Validate request
    ↓
Find Event
    ↓
Event exists?
    ├── NO → throw BusinessException
    └── YES
            ↓
Validate category
            ↓
Update event
            ↓
Save
            ↓
Return updated event
```

---

## 3.5 Admin - Delete Event

```text
Admin
    ↓
DELETE /api/v1/admin/events/{id}
    ↓
Find Event
    ↓
Event exists?
    ├── NO → throw BusinessException
    └── YES
            ↓
Has Event Sessions?
            ├── YES
            │      ↓
            │  Mark event as INACTIVE
            │
            └── NO
                   ↓
              Delete Event
                   ↓
              Return success
```

---

## 3.6 Public - Search Events

```text
Client
    ↓
Enter keyword
    ↓
GET /events?keyword=...
    ↓
Search by title
    ↓
Return matching events
```

---

## 3.7 Public - Filter by Category

```text
Client
    ↓
Choose category
    ↓
GET /events?categoryId=...
    ↓
Filter events
    ↓
Return filtered list
```

---

# 4. Functional Requirements

### FR-01

Allow public users to retrieve all active events.

---

### FR-02

Allow public users to retrieve event details by id.

---

### FR-03

Allow public users to search events by keyword.

---

### FR-04

Allow public users to filter events by category.

---

### FR-05

Allow public users to retrieve paginated event lists.

---

### FR-06

Allow administrators to create new events.

---

### FR-07

Allow administrators to update existing events.

---

### FR-08

Allow administrators to delete events that have no associated event sessions.

---

### FR-09

If an event already has one or more event sessions, the system must not physically delete it.

Instead, the event should be marked as **INACTIVE** to preserve historical booking data.

---

### FR-10

The system must verify that the referenced category exists before creating or updating an event.

---

### FR-11

Only users with the **ADMIN** role may create, update, delete, activate, or deactivate events.

---

### FR-12

All API responses must follow the project's standard `ApiResponse<T>` format.

---

### FR-13

Validation errors must be handled globally using the project's Global Exception Handler.

---

### FR-14

The Event Management module must not modify Event Session, Seat, Booking, or Payment data.

# 5. Business Rules

### BR-01

Event title is required.

---

### BR-02

Event title must be unique (case-insensitive).

Example

```
Concert Night
```

and

```
concert night
```

are considered duplicates.

---

### BR-03

Leading and trailing spaces of the title must be trimmed before validation and persistence.

---

### BR-04

Category must exist before an event can be created or updated.

---

### BR-05

Only ACTIVE categories can be assigned to an event.

---

### BR-06

Event description is required.

---

### BR-07

Poster URL is required.

Poster must be a valid URL.

---

### BR-08

Banner URL is optional.

If provided, it must be a valid URL.

---

### BR-09

Sale start time must be earlier than sale end time.

---

### BR-10

An event must have one of the following statuses:

- ACTIVE
- INACTIVE

---

### BR-11

Public users can only view ACTIVE events.

INACTIVE events are visible only to ADMIN users.

---

### BR-12

Only users with the ADMIN role may:

- create events
- update events
- delete events
- change event status

---

### BR-13

If an event already has one or more Event Sessions, it must not be physically deleted.

Instead, update:

```
status = INACTIVE
```

This preserves booking history and referential integrity.

---

### BR-14

Deleting an event without Event Sessions permanently removes the record.

---

### BR-15

Updating an event must not modify:

- Event Sessions
- Seats
- Bookings
- Payments

These modules are managed independently.

---

### BR-16

All business validation failures must throw BusinessException (or project-specific business exceptions).

---

### BR-17

Every successful create or update operation must update the entity's `updated_at` timestamp automatically.

---

### BR-18

The Event Management module must not create Event Sessions automatically.

Creating schedules belongs to US11 – Event Session Management.

---

# 6. API Contract

---

## 6.1 Get Event List

### Endpoint

```
GET /api/v1/events
```

### Query Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| keyword | No | Search by event title |
| categoryId | No | Filter by category |
| status | No | Filter by status (ADMIN only) |
| page | No | Page number |
| size | No | Page size |
| sort | No | Sort field |

### Response

```json
{
  "success": true,
  "message": "Events retrieved successfully.",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Taylor Swift - The Eras Tour",
        "posterUrl": "https://example.com/poster.jpg",
        "categoryName": "Concert",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  },
  "timestamp": "2026-08-07T20:00:00"
}
```

---

## 6.2 Get Event Detail

### Endpoint

```
GET /api/v1/events/{id}
```

### Response

```json
{
  "success": true,
  "message": "Event retrieved successfully.",
  "data": {
    "id": 1,
    "title": "Taylor Swift - The Eras Tour",
    "description": "World Tour 2026",
    "posterUrl": "https://example.com/poster.jpg",
    "bannerUrl": "https://example.com/banner.jpg",
    "saleStartTime": "2026-08-10T09:00:00",
    "saleEndTime": "2026-08-20T23:59:59",
    "status": "ACTIVE",
    "category": {
      "id": 2,
      "name": "Concert"
    }
  },
  "timestamp": "2026-08-07T20:00:00"
}
```

---

## 6.3 Create Event

### Endpoint

```
POST /api/v1/admin/events
```

### Request

```json
{
  "title": "Taylor Swift - The Eras Tour",
  "description": "World Tour 2026",
  "categoryId": 2,
  "posterUrl": "https://example.com/poster.jpg",
  "bannerUrl": "https://example.com/banner.jpg",
  "saleStartTime": "2026-08-10T09:00:00",
  "saleEndTime": "2026-08-20T23:59:59",
  "status": "ACTIVE"
}
```

### Response

```json
{
  "success": true,
  "message": "Event created successfully.",
  "data": {
    "id": 1,
    "title": "Taylor Swift - The Eras Tour",
    "status": "ACTIVE"
  },
  "timestamp": "2026-08-07T20:00:00"
}
```

---

## 6.4 Update Event

### Endpoint

```
PUT /api/v1/admin/events/{id}
```

### Request

```json
{
  "title": "Taylor Swift - Asia Tour",
  "description": "Updated Description",
  "categoryId": 2,
  "posterUrl": "https://example.com/poster.jpg",
  "bannerUrl": "https://example.com/banner.jpg",
  "saleStartTime": "2026-08-10T09:00:00",
  "saleEndTime": "2026-08-25T23:59:59",
  "status": "ACTIVE"
}
```

### Response

```json
{
  "success": true,
  "message": "Event updated successfully.",
  "data": {
    "id": 1,
    "title": "Taylor Swift - Asia Tour",
    "status": "ACTIVE"
  },
  "timestamp": "2026-08-07T20:00:00"
}
```

---

## 6.5 Delete Event

### Endpoint

```
DELETE /api/v1/admin/events/{id}
```

### Response

```json
{
  "success": true,
  "message": "Event deleted successfully.",
  "data": null,
  "timestamp": "2026-08-07T20:00:00"
}
```

---

## 6.6 Activate Event

### Endpoint

```
PATCH /api/v1/admin/events/{id}/activate
```

### Response

```json
{
  "success": true,
  "message": "Event activated successfully.",
  "data": null,
  "timestamp": "2026-08-07T20:00:00"
}
```

---

## 6.7 Deactivate Event

### Endpoint

```
PATCH /api/v1/admin/events/{id}/deactivate
```

### Response

```json
{
  "success": true,
  "message": "Event deactivated successfully.",
  "data": null,
  "timestamp": "2026-08-07T20:00:00"
}
```

# 7. Validation Rules

## Create Event Request

### title

- required
- must not be blank
- maximum 255 characters
- must be unique (case-insensitive)
- trim leading and trailing spaces

---

### description

- required
- must not be blank
- maximum 5000 characters

---

### categoryId

- required
- must reference an existing ACTIVE category

---

### posterUrl

- required
- must not be blank
- must be a valid URL
- maximum 500 characters

---

### bannerUrl

- optional
- if provided, must be a valid URL
- maximum 500 characters

---

### saleStartTime

- required

---

### saleEndTime

- required
- must be later than saleStartTime

---

### status

- optional
- default value: ACTIVE
- accepted values:
  - ACTIVE
  - INACTIVE

---

## Update Event Request

Validation rules are the same as Create Event Request except:

- Event must already exist.
- Duplicate title check must ignore the current event.

Suggested repository query:

```
existsByTitleIgnoreCaseAndIdNot(...)
```

---

## Query Parameters

### keyword

- optional
- maximum 100 characters

---

### categoryId

- optional
- category must exist if provided

---

### page

- optional
- minimum = 0

---

### size

- optional
- minimum = 1
- maximum = 100

---

### sort

Allowed values depend on project convention.

Recommended:

- title
- createdAt
- updatedAt

---

# 8. Database Impact

## Read

Tables involved

- event
- category
- event_session (delete safety check only)

---

## Insert

Table

- event

---

## Update

Table

- event

---

## Delete

Table

- event

Only when:

- Event exists.
- No EventSession references this event.

Otherwise:

```
status = INACTIVE
```

---

## Suggested Repository Methods

### EventRepository

```
findAll(...)

findById(...)

existsByTitleIgnoreCase(...)

existsByTitleIgnoreCaseAndIdNot(...)

findByStatus(...)

findByCategoryId(...)

search(...)
```

---

### CategoryRepository

```
findById(...)

existsById(...)
```

---

### EventSessionRepository

```
existsByEventId(...)
```

Used only before deleting an Event.

---

# 9. Expected Classes

## Controller

```
EventController
```

---

## Service

```
EventService

EventServiceImpl
```

---

## Repository

```
EventRepository

CategoryRepository

EventSessionRepository
```

---

## DTO

```
CreateEventRequest

UpdateEventRequest

EventSummaryResponse

EventDetailResponse
```

---

## Mapper

```
EventMapper
```

---

## Entity

```
Event
```

---

## Exception Classes

Reuse existing project exceptions whenever possible.

Recommended:

```
EventNotFoundException

EventAlreadyExistsException

CategoryNotFoundException

CategoryInactiveException

EventHasSessionsException
```

If the project uses a centralized `BusinessException`, prefer reusing it instead of creating many small exception classes.

---

# 10. Transaction

## Create Event

Transactional.

Flow

```
Validate Request

↓

Validate Category

↓

Create Event

↓

Save Event
```

Rollback when

- database error
- duplicate title
- invalid category

---

## Update Event

Transactional.

Flow

```
Find Event

↓

Validate Request

↓

Update Fields

↓

Save
```

Rollback when

- event not found
- duplicate title
- invalid category
- persistence failure

---

## Delete Event

Transactional.

Flow

```
Find Event

↓

Check Event Sessions

↓

Delete
```

Rollback when

- event not found
- event has Event Sessions
- database failure

---

## Activate / Deactivate Event

Transactional.

Flow

```
Find Event

↓

Update Status

↓

Save
```

Rollback when

- event not found
- persistence failure

---

# 11. Exception Cases

Throw `BusinessException` (or project-specific business exceptions) in the following situations.

| Case | Exception |
|------|-----------|
| Event not found | EventNotFoundException |
| Duplicate event title | EventAlreadyExistsException |
| Category not found | CategoryNotFoundException |
| Category inactive | CategoryInactiveException |
| Invalid sale time | BusinessException |
| Event already has sessions | EventHasSessionsException |
| Invalid URL | ValidationException |
| Unauthorized access | AccessDeniedException |
| Validation failure | MethodArgumentNotValidException |

---

## Global Exception Handling

Validation exceptions should be handled by the existing GlobalExceptionHandler.

Business exceptions should return the project's standard `ApiResponse<Void>` (or `ApiResponse<?>`) with appropriate HTTP status codes.

No controller should manually catch business exceptions.

All business validation must be handled inside the Service layer.
# 12. Acceptance Criteria

The implementation is considered complete when all of the following conditions are satisfied.

### AC-01

Public users can retrieve a paginated list of ACTIVE events.

---

### AC-02

Public users can search events by keyword.

---

### AC-03

Public users can filter events by category.

---

### AC-04

Public users can retrieve event details by id.

---

### AC-05

ADMIN users can create new events.

---

### AC-06

ADMIN users can update existing events.

---

### AC-07

ADMIN users can delete events that are not referenced by any Event Session.

---

### AC-08

If an Event already has one or more Event Sessions, DELETE must be rejected.

The administrator should deactivate the Event instead.

---

### AC-09

Category validation is performed before every create or update operation.

---

### AC-10

Duplicate event titles are rejected.

---

### AC-11

All validation errors are handled by the GlobalExceptionHandler.

---

### AC-12

All responses follow the project's standard `ApiResponse<T>` format.

---

### AC-13

Only ADMIN users may access management endpoints.

---

### AC-14

The Event module does not modify EventSession, Seat, Booking, or Payment data.

---

# 13. Coding Constraints

The implementation must comply with all project conventions.

Follow:

- PROJECT_CONTEXT.md
- DATABASE.md
- Existing package structure

---

Always use

- Constructor Injection
- Lombok `@RequiredArgsConstructor`
- Jakarta Validation
- Spring Data JPA

---

Never use

- Field Injection (`@Autowired`)
- RuntimeException directly
- Business logic inside Controller
- Native SQL unless absolutely necessary

---

Business logic belongs in

```
Service Layer
```

---

Validation belongs in

```
DTO + Jakarta Validation
```

---

Persistence belongs in

```
Repository
```

---

Controllers should

- remain thin
- only orchestrate request and response
- never contain business logic

---

All APIs must return

```
ApiResponse<T>
```

---

Use

```
BusinessException
```

for business rule violations.

---

Do not

- modify completed Flyway migrations
- change unrelated modules
- introduce unnecessary abstractions
- duplicate existing code

---

Generate

- clean
- readable
- maintainable
- production-ready

code.

---

# 14. Review Checklist

Before considering the implementation complete, verify:

## General

- Event entity follows project conventions.
- Controller is thin.
- Business logic exists only in Service.
- Repository methods are minimal and reusable.

---

## Validation

- Required fields are validated.
- Duplicate title validation works.
- Category existence validation works.
- Sale time validation works.
- URL validation works.

---

## Security

- Public APIs are publicly accessible.
- Admin APIs require ADMIN role.
- Unauthorized requests are rejected correctly.

---

## Business Logic

- Event can be created successfully.
- Event can be updated successfully.
- Event detail returns correct data.
- Search works correctly.
- Category filtering works correctly.
- Pagination works correctly.

---

## Delete Logic

- Event without Event Sessions can be deleted.
- Event with Event Sessions cannot be deleted.
- Appropriate BusinessException is thrown.

---

## API

- HTTP methods follow REST conventions.
- Response format follows ApiResponse<T>.
- Proper HTTP status codes are returned.

---

## Code Quality

- No duplicated logic.
- No unnecessary repository queries.
- No dead code.
- No hardcoded values.
- Meaningful variable names.
- Proper exception messages.

---

# 15. Architecture Decisions

After generating the code, always include an **Architecture Review**.

For every important implementation decision, explain the following.

---

## Decision

What decision was made?

---

## Why

Why was this approach chosen?

---

## Alternative

What other implementation could have been used?

---

## Trade-off

Advantages and disadvantages.

---

## Future Improvement

How could this be improved in a larger production system?

---

## Recommended Architecture Decisions

The implementation should explain decisions similar to the following.

### Decision 1 — Separate Event from Event Session

**Why**

An Event represents general information.

Schedules belong to Event Sessions.

This avoids duplicated event information across multiple schedules.

---

### Decision 2 — Validate Category before saving Event

**Why**

Prevent orphan foreign keys.

Guarantee referential integrity.

---

### Decision 3 — Reject DELETE when Event has Event Sessions

**Why**

Booking history must remain valid.

Physical deletion would break referential integrity.

---

### Decision 4 — Use BusinessException instead of RuntimeException

**Why**

Keep business errors consistent.

Allow centralized error handling.

---

### Decision 5 — Thin Controller + Service Layer

**Why**

Follow Single Responsibility Principle.

Improve maintainability and testability.

---

# 16. Knowledge Check

After implementation, explain the solution as if mentoring a Java Backend Intern.

Include the following topics.

---

## Spring Features Used

Explain why the implementation uses:

- Spring MVC
- Spring Data JPA
- Spring Security
- Jakarta Validation
- Transaction Management
- Global Exception Handling
- Lombok

---

## Design Patterns Used

Explain patterns used in this module.

Examples

- Service Layer
- Repository Pattern
- DTO Pattern
- Builder Pattern (Lombok)
- Dependency Injection
- Guard Clause

---

## Why This Design?

Explain why this implementation is better than placing all logic inside the Controller.

Discuss:

- maintainability
- readability
- scalability
- testability

---

## Possible Interview Questions

Examples

- Why separate Event and EventSession?
- Why validate Category before creating an Event?
- Why use BusinessException instead of RuntimeException?
- Why should Controllers remain thin?
- Why is Event deletion restricted after Event Sessions exist?
- Why use DTOs instead of exposing Entities directly?
- Why use @Transactional in Service methods?

---

## Common Mistakes

Explain common mistakes developers make.

Examples

- Performing business logic in Controllers.
- Returning Entity objects directly.
- Forgetting to validate referenced Category.
- Allowing duplicate Event titles.
- Physically deleting Events referenced by Event Sessions.
- Missing transaction boundaries.
- Writing unnecessary repository queries.
- Mixing Event logic with Event Session logic.
- Ignoring RESTful API conventions.

---

## Important Parts to Understand

The AI should briefly explain:

- Event lifecycle
- Validation flow
- CRUD workflow
- Delete safety mechanism
- Category relationship
- Layer responsibilities
- Transaction boundaries