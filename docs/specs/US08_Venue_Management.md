# US08 - Venue Management

---

# 1. Objective

Implement Venue Management for the Ticket Booking System.

A Venue represents a physical location where events are held.

Examples:

- CGV Vincom Royal City
- National Convention Center
- My Dinh National Stadium

Venue Management provides CRUD operations for administrators and allows public users to view venue information.

---

# 2. Scope

## Included

- Get all venues
- Get venue by id
- Create venue
- Update venue
- Delete venue
- Validate duplicate venue name
- Prevent deleting venues that are referenced by Event Sessions
- Prevent reducing capacity below the existing number of seats

## Not Included

- Seat Management
- Event Management
- Event Session Management
- Venue images
- Venue map
- Pagination unless already implemented

---

# 3. Business Flow

## 3.1 Get All Venues

```
Client
    ↓
GET /api/v1/venues
    ↓
Load all venues
    ↓
Return venue list
```

---

## 3.2 Get Venue Detail

```
Client
    ↓
GET /api/v1/venues/{id}
    ↓
Find venue
    ↓
Venue exists?
    ├── NO → BusinessException
    └── YES
            ↓
Return venue
```

---

## 3.3 Create Venue

```
Admin
    ↓
POST /api/v1/admin/venues
    ↓
Validate request
    ↓
Check duplicate name
    ↓
Save venue
    ↓
Return created venue
```

---

## 3.4 Update Venue

```
Admin
    ↓
PUT /api/v1/admin/venues/{id}
    ↓
Find venue
    ↓
Venue exists?
    ├── NO → BusinessException
    └── YES
            ↓
Validate duplicate name
            ↓
Validate capacity
            ↓
Update venue
            ↓
Return updated venue
```

---

## 3.5 Delete Venue

```
Admin
    ↓
DELETE /api/v1/admin/venues/{id}
    ↓
Find venue
    ↓
Venue exists?
    ├── NO → BusinessException
    └── YES
            ↓
Has Event Sessions?
            ├── YES → BusinessException
            └── NO
                    ↓
Has Seats?
            ├── YES → BusinessException
            └── NO
                    ↓
Delete venue
                    ↓
Return success
```

---

# 4. Functional Requirements

FR-01

Public users can retrieve all venues.

FR-02

Public users can retrieve venue details.

FR-03

Administrators can create venues.

FR-04

Administrators can update venues.

FR-05

Administrators can delete venues.

FR-06

Venue names must be unique.

FR-07

Prevent deleting venues referenced by Event Sessions.

FR-08

Prevent reducing capacity below the current seat count.

FR-09

All responses use ApiResponse<T>.

---

# 5. Business Rules

BR-01

Venue name is required.

BR-02

Venue name must be unique (ignore case and trim spaces).

BR-03

Address is required.

BR-04

Capacity must be greater than zero.

BR-05

Capacity cannot be smaller than the number of existing seats.

BR-06

Venue cannot be deleted if any Event Session references it.

BR-07

Venue cannot be deleted if seats already exist.

BR-08

Only ADMIN can create, update and delete venues.

---

# 6. API Contract

## Get All Venues

```
GET /api/v1/venues
```

Response

```json
{
  "success": true,
  "message": "Venues retrieved successfully.",
  "data": [
    {
      "id": 1,
      "name": "CGV Vincom",
      "address": "Ha Noi",
      "capacity": 250
    }
  ]
}
```

---

## Get Venue Detail

```
GET /api/v1/venues/{id}
```

---

## Create Venue

```
POST /api/v1/admin/venues
```

Request

```json
{
  "name": "CGV Vincom",
  "address": "Ha Noi",
  "capacity": 250
}
```

---

## Update Venue

```
PUT /api/v1/admin/venues/{id}
```

---

## Delete Venue

```
DELETE /api/v1/admin/venues/{id}
```

---

# 7. Validation Rules

CreateVenueRequest

- name: @NotBlank
- name: maximum length follows database schema
- address: @NotBlank
- address: maximum length follows database schema
- capacity: @Positive

UpdateVenueRequest

Same validation as CreateVenueRequest.

---

# 8. Database Impact

Read

- venue
- seat
- event_session

Insert

- venue

Update

- venue

Delete

- venue

Repository Queries

VenueRepository

- findAll()
- findById()
- existsByNameIgnoreCase()
- existsByNameIgnoreCaseAndIdNot()

SeatRepository

- countByVenueId()

EventSessionRepository

- existsByVenueId()

---

# 9. Expected Classes

Controller

- VenueController

Service

- VenueService
- VenueServiceImpl

DTO

- CreateVenueRequest
- UpdateVenueRequest
- VenueResponse

Repository

- VenueRepository

Mapper

- VenueMapper

Entity

- Venue

---

# 10. Transaction

Create Venue

@Transactional

Update Venue

@Transactional

Delete Venue

@Transactional

Rollback when:

- Repository failure
- Duplicate venue
- Capacity validation failure
- Venue in use

---

# 11. Exception Cases

Throw BusinessException when:

- Venue not found
- Venue already exists
- Invalid capacity
- Venue is referenced by Event Sessions
- Venue contains seats
- User is not ADMIN

---

# 12. Acceptance Criteria

- Public users can list venues.
- Public users can view venue details.
- Admin can create venues.
- Duplicate venue names are rejected.
- Admin can update venues.
- Capacity validation works correctly.
- Venue cannot be deleted while referenced by Event Sessions.
- Venue cannot be deleted while seats exist.
- Validation errors are handled globally.
- Responses follow ApiResponse<T>.

---

# 13. Coding Constraints

- Follow PROJECT_CONTEXT.md
- Follow DATABASE.md
- Keep existing package structure
- Use constructor injection only
- Use ApiResponse<T>
- Use BusinessException
- Use Jakarta Validation
- Keep controllers thin
- Put business logic in Service
- Do not modify unrelated modules
- Do not modify existing Flyway migrations
- Generate production-ready code