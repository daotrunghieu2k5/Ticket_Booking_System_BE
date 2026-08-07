# US09 - Seat Management

---

# 1. Objective

Implement Seat Management for the Ticket Booking System.

A Seat represents a physical seat inside a Venue.

Seats belong to a specific Venue and are reused across multiple Event Sessions.

This user story provides administrators with the ability to manage seat layouts while allowing the system to reuse those seats for booking in future user stories.

---

# 2. Scope

## Included

- Get all seats of a venue
- Get seat by id
- Create a seat
- Update a seat
- Delete a seat
- Batch create seats
- Batch delete seats
- Validate duplicate seat position
- Validate venue existence
- Prevent deleting seats that are already referenced by bookings
- Prevent deleting seats that are currently locked (future compatibility)

## Not Included

- Seat availability
- Seat reservation
- Seat selection
- Seat Hold
- Booking
- Payment
- Dynamic seat pricing
- Seat map visualization
- Event Session management

---

# 3. Business Flow

## 3.1 Get Seats By Venue

```text
Client
    ↓
GET /api/v1/venues/{venueId}/seats
    ↓
Validate venue
    ↓
Load all seats
    ↓
Return seat list
```

---

## 3.2 Get Seat Detail

```text
Client
    ↓
GET /api/v1/seats/{id}
    ↓
Find seat
    ↓
Seat exists?
    ├── NO → BusinessException
    └── YES
            ↓
Return seat detail
```

---

## 3.3 Create Seat

```text
Admin
    ↓
POST /api/v1/admin/venues/{venueId}/seats
    ↓
Validate request
    ↓
Validate venue
    ↓
Check duplicate seat
    ↓
Create seat
    ↓
Save seat
    ↓
Return created seat
```

---

## 3.4 Batch Create Seats

```text
Admin
    ↓
POST /api/v1/admin/venues/{venueId}/seats/batch
    ↓
Validate venue
    ↓
Validate row range
    ↓
Validate seat range
    ↓
Generate seat list
    ↓
Check duplicates
    ↓
Save all seats
    ↓
Return success
```

Example

Input

Rows

A - J

Seat Numbers

1 - 20

Generated

A1
A2
...
A20

B1
...
J20

---

## 3.5 Update Seat

```text
Admin
    ↓
PUT /api/v1/admin/seats/{id}
    ↓
Find seat
    ↓
Seat exists?
    ├── NO → BusinessException
    └── YES
            ↓
Check duplicate position
            ↓
Update seat
            ↓
Save
            ↓
Return updated seat
```

---

## 3.6 Delete Seat

```text
Admin
    ↓
DELETE /api/v1/admin/seats/{id}
    ↓
Find seat
    ↓
Seat exists?
    ├── NO → BusinessException
    └── YES
            ↓
Referenced by Booking?
            ├── YES → BusinessException
            └── NO
                    ↓
Locked by Seat Hold?
            ├── YES → BusinessException
            └── NO
                    ↓
Delete seat
                    ↓
Return success
```

---

## 3.7 Batch Delete Seats

```text
Admin
    ↓
POST /api/v1/admin/seats/batch-delete
    ↓
Validate seat ids
    ↓
Check booking references
    ↓
Delete all valid seats
    ↓
Return success
```

---

# 4. Functional Requirements

FR-01

Allow public users to retrieve all seats of a venue.

FR-02

Allow public users to retrieve seat details.

FR-03

Allow administrators to create a new seat.

FR-04

Allow administrators to batch create seats.

FR-05

Allow administrators to update seat information.

FR-06

Allow administrators to delete seats.

FR-07

Allow administrators to batch delete seats.

FR-08

Prevent duplicate seat positions inside the same venue.

FR-09

Validate venue existence before creating seats.

FR-10

Prevent deleting seats that have already been used by bookings.

FR-11

Prevent deleting seats currently locked by Seat Hold.

FR-12

Return all responses using the standard ApiResponse<T> format.
# 5. Business Rules

BR-01

A Seat must belong to an existing Venue.

A seat cannot exist without a venue.

---

BR-02

Seat row is required.

Examples:

A

B

C

AA

BB

---

BR-03

Seat number must be greater than zero.

Examples

Valid

1

2

15

100

Invalid

0

-1

---

BR-04

The combination of:

- venue
- row
- seat number

must be unique.

Example

Venue A

A1 ✅

A2 ✅

A3 ✅

A1 ❌ Duplicate

The same seat position may exist in another venue.

Example

Venue A

A1

Venue B

A1

This is allowed.

---

BR-05

Seat type is required.

Supported seat types are defined by the SeatType enum.

Current supported values:

- STANDARD
- VIP
- COUPLE
- WHEELCHAIR

Future seat types may be added without changing existing business logic.

---

BR-06

A Seat can only be updated if the new seat position does not conflict with another seat in the same venue.

Example

Existing seats

A1

A2

A3

Updating

A3 → A2

Result

Rejected because A2 already exists.

---

BR-07

A Seat that has already been referenced by a BookingItem must not be deleted.

This ensures booking history remains valid.

---

BR-08

A Seat that is currently locked by a Seat Hold must not be deleted.

Although Seat Hold is implemented in a later user story, the service should be designed to support this validation.

---

BR-09

Batch seat creation must skip duplicate seat positions within the request.

Example

Input

A1

A2

A2

A3

Result

Validation error.

Duplicate seat positions are not allowed.

---

BR-10

Batch seat creation must also reject seats that already exist in the database.

Example

Database

A1

A2

Request

A2

A3

Result

Rejected because A2 already exists.

---

BR-11

Batch delete should fail if any selected seat cannot be deleted.

The operation must be atomic.

If one seat fails validation, no seats are deleted.

---

BR-12

Only users with the ADMIN role can create, update, or delete seats.

Public users may only retrieve seat information.

---

BR-13

All business validation failures must throw BusinessException.

Do not expose database exceptions directly to clients.

---

BR-14

All create, update, and delete operations must be executed within a transaction to ensure data consistency.
# 6. API Contract

---

## 6.1 Get Seats By Venue

### Endpoint

```
GET /api/v1/venues/{venueId}/seats
```

### Description

Retrieve all seats belonging to a specific venue.

### Path Variable

| Name | Type | Required |
|------|------|----------|
| venueId | Long | Yes |

### Response

```json
{
  "success": true,
  "message": "Seats retrieved successfully.",
  "data": [
    {
      "id": 1,
      "rowName": "A",
      "seatNumber": 1,
      "seatCode": "A1",
      "seatType": "STANDARD",
      "active": true
    },
    {
      "id": 2,
      "rowName": "A",
      "seatNumber": 2,
      "seatCode": "A2",
      "seatType": "VIP",
      "active": true
    }
  ],
  "timestamp": "2026-08-08T10:00:00"
}
```

---

## 6.2 Get Seat Detail

### Endpoint

```
GET /api/v1/seats/{id}
```

### Response

```json
{
  "success": true,
  "message": "Seat retrieved successfully.",
  "data": {
    "id": 1,
    "venueId": 1,
    "rowName": "A",
    "seatNumber": 1,
    "seatCode": "A1",
    "seatType": "STANDARD",
    "active": true
  },
  "timestamp": "2026-08-08T10:00:00"
}
```

---

## 6.3 Create Seat

### Endpoint

```
POST /api/v1/admin/venues/{venueId}/seats
```

### Request

```json
{
  "rowName": "A",
  "seatNumber": 1,
  "seatType": "STANDARD"
}
```

### Response

```json
{
  "success": true,
  "message": "Seat created successfully.",
  "data": {
    "id": 1,
    "venueId": 1,
    "rowName": "A",
    "seatNumber": 1,
    "seatCode": "A1",
    "seatType": "STANDARD",
    "active": true
  },
  "timestamp": "2026-08-08T10:00:00"
}
```

---

## 6.4 Batch Create Seats

### Endpoint

```
POST /api/v1/admin/venues/{venueId}/seats/batch
```

### Request

```json
{
  "startRow": "A",
  "endRow": "J",
  "startSeatNumber": 1,
  "endSeatNumber": 20,
  "seatType": "STANDARD"
}
```

### Generated Seats

```
A1
A2
...
A20

B1
...
J20
```

### Response

```json
{
  "success": true,
  "message": "Seats created successfully.",
  "data": {
    "createdCount": 200
  },
  "timestamp": "2026-08-08T10:00:00"
}
```

---

## 6.5 Update Seat

### Endpoint

```
PUT /api/v1/admin/seats/{id}
```

### Request

```json
{
  "rowName": "B",
  "seatNumber": 5,
  "seatType": "VIP",
  "active": true
}
```

### Response

```json
{
  "success": true,
  "message": "Seat updated successfully.",
  "data": {
    "id": 1,
    "venueId": 1,
    "rowName": "B",
    "seatNumber": 5,
    "seatCode": "B5",
    "seatType": "VIP",
    "active": true
  },
  "timestamp": "2026-08-08T10:00:00"
}
```

---

## 6.6 Delete Seat

### Endpoint

```
DELETE /api/v1/admin/seats/{id}
```

### Response

```json
{
  "success": true,
  "message": "Seat deleted successfully.",
  "data": null,
  "timestamp": "2026-08-08T10:00:00"
}
```

---

## 6.7 Batch Delete Seats

### Endpoint

```
POST /api/v1/admin/seats/batch-delete
```

### Request

```json
{
  "seatIds": [
    1,
    2,
    3,
    4
  ]
}
```

### Response

```json
{
  "success": true,
  "message": "Seats deleted successfully.",
  "data": {
    "deletedCount": 4
  },
  "timestamp": "2026-08-08T10:00:00"
}
```
# 7. Validation Rules

## 7.1 Create Seat Request

### venueId

- Must refer to an existing Venue.
- Validation is performed in the Service layer.

---

### rowName

- Required.
- Must not be blank.
- Trim leading and trailing spaces.
- Maximum length follows the database schema.
- Only alphabetic characters are allowed.
- Convert to uppercase before saving.

Examples

Valid

```
A
B
AA
VIP
```

Invalid

```
""
" "
A1
A@
```

---

### seatNumber

- Required.
- Must be greater than zero.
- Maximum value should be reasonable (project-defined limit).

Examples

Valid

```
1
15
100
```

Invalid

```
0
-1
```

---

### seatType

- Required.
- Must be one of the values defined in SeatType enum.

Supported values

```
STANDARD
VIP
COUPLE
WHEELCHAIR
```

---

## 7.2 Update Seat Request

Validation rules are the same as Create Seat Request.

Additional validation:

- Seat must exist.
- The updated seat position must not conflict with another seat in the same venue.

---

## 7.3 Batch Create Seat Request

### startRow

- Required.
- Must not be blank.
- Must be alphabetic characters only.

---

### endRow

- Required.
- Must not be blank.
- Must be alphabetic characters only.
- Must not be before startRow.

Example

Valid

```
A → J
```

Invalid

```
J → A
```

---

### startSeatNumber

- Required.
- Must be greater than zero.

---

### endSeatNumber

- Required.
- Must be greater than or equal to startSeatNumber.

Example

Valid

```
1 → 20
```

Invalid

```
20 → 1
```

---

### seatType

- Required.
- Must exist in SeatType enum.

---

## 7.4 Batch Delete Request

### seatIds

- Required.
- Must not be empty.
- Duplicate IDs are not allowed.
- Every seat must exist.

---

## 7.5 Business Validation

The following validations cannot be handled by Jakarta Validation and must be implemented in the Service layer.

### Duplicate Seat Position

The combination of:

- venueId
- rowName
- seatNumber

must be unique.

---

### Venue Exists

The specified venue must exist.

---

### Seat Exists

The specified seat must exist before update or delete.

---

### Booking Reference Check

A seat that has already been referenced by a BookingItem cannot be deleted.

---

### Seat Hold Check

A seat currently locked by Seat Hold cannot be deleted.

(This rule is reserved for future user stories.)

---

### Batch Atomic Validation

If any validation fails during batch create or batch delete, the entire operation must be rolled back.

No partial success is allowed.
# 8. Database Impact

## Read

- venue
- seat
- booking_item (delete validation)
- seat_hold (future compatibility)

## Insert

- seat

## Update

- seat

## Delete

- seat

Only if the seat is not referenced by any BookingItem.

---

## Suggested Repository Queries

### SeatRepository

```java
List<Seat> findByVenueIdOrderByRowNameAscSeatNumberAsc(Long venueId);

Optional<Seat> findById(Long id);

boolean existsByVenueIdAndRowNameIgnoreCaseAndSeatNumber(
        Long venueId,
        String rowName,
        Integer seatNumber
);

boolean existsByVenueIdAndRowNameIgnoreCaseAndSeatNumberAndIdNot(
        Long venueId,
        String rowName,
        Integer seatNumber,
        Long id
);

List<Seat> saveAll(List<Seat> seats);

void deleteAllById(Iterable<Long> ids);
```

### VenueRepository

```java
boolean existsById(Long id);

Optional<Venue> findById(Long id);
```

### BookingItemRepository

```java
boolean existsBySeatId(Long seatId);
```

### SeatHoldRepository

Reserved for future user stories.

```java
boolean existsBySeatId(Long seatId);
```

---

# 9. Expected Classes

## Controller

```
SeatController
```

---

## Service

```
SeatService
SeatServiceImpl
```

---

## DTO

```
CreateSeatRequest

UpdateSeatRequest

BatchCreateSeatRequest

BatchDeleteSeatRequest

SeatResponse
```

---

## Repository

```
SeatRepository

VenueRepository

BookingItemRepository
```

---

## Mapper

```
SeatMapper
```

---

## Entity

```
Seat
```

---

## Enum

```
SeatType
```

Supported values

```
STANDARD
VIP
COUPLE
WHEELCHAIR
```

---

# 10. Transaction

## Create Seat

Should be executed within a transaction.

Rollback when

- duplicate seat
- venue not found
- repository failure

---

## Update Seat

Should be transactional.

Rollback when

- duplicate seat position
- seat not found
- repository failure

---

## Batch Create

Must be transactional.

If any generated seat fails validation,
the entire batch must be rolled back.

Partial creation is not allowed.

---

## Delete Seat

Must be transactional.

Rollback when

- booking reference exists
- seat hold exists
- repository failure

---

## Batch Delete

Must be transactional.

If one seat cannot be deleted,
the entire batch operation must fail.

---

# 11. Exception Cases

Throw BusinessException (or project-specific business exceptions) when:

- venue does not exist
- seat does not exist
- duplicate seat position
- seat already referenced by BookingItem
- seat currently locked by Seat Hold
- invalid batch range
- invalid row range
- invalid seat number range
- user does not have ADMIN permission

Validation errors should be handled globally using the existing GlobalExceptionHandler.

---

# 12. Acceptance Criteria

- Public users can retrieve seats by venue.
- Public users can retrieve seat details.
- ADMIN can create seats.
- ADMIN can update seats.
- ADMIN can delete seats.
- ADMIN can batch create seats.
- ADMIN can batch delete seats.
- Duplicate seat positions are rejected.
- Seat codes are generated automatically.
- Delete validation against BookingItem works.
- Responses follow ApiResponse<T>.
- Validation errors are handled globally.
- Controller contains no business logic.
- Service contains all business logic.
- Repository only performs data access.
- All write operations are transactional.

---

# 13. Coding Constraints

- Follow PROJECT_CONTEXT.md.
- Follow DATABASE.md.
- Keep the existing package structure.
- Use constructor injection only.
- Do not use field injection.
- Use ApiResponse<T>.
- Use BusinessException.
- Use Jakarta Validation.
- Use @Transactional where appropriate.
- Keep controllers thin.
- Place business logic inside the Service layer.
- Do not modify unrelated modules.
- Do not change completed Flyway migrations.
- Generate production-ready code.
- Prefer readable and maintainable code.
- Do not introduce unnecessary abstractions.
- Do not generate unused classes or methods.
- Reuse existing common utilities whenever possible.