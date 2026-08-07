# DATABASE.md

# Ticket Booking System Database Documentation

---

# 1. Database Overview

## Database Engine

- PostgreSQL

## ORM Framework

- Spring Data JPA (Hibernate)

## Migration Tool

- Flyway

## Primary Key Strategy

- BIGSERIAL

## Naming Convention

Table

- Singular noun
- snake_case

Examples

```
users
booking
event_session
payment_transaction
```

Columns

- snake_case

Examples

```
full_name
created_at
event_session_id
```

Foreign Key

```
xxx_id
```

Examples

```
user_id
booking_id
payment_id
```

Audit Columns

All business entities should contain

```
created_at
updated_at
```

unless the table is only used for temporary data.

---

# 2. Database Design Rules

## General Rules

- Every entity uses BIGSERIAL as primary key.
- Every foreign key must enforce referential integrity.
- Every business entity extends BaseEntity unless otherwise specified.
- Use LocalDateTime for timestamp fields.
- Store enums as VARCHAR.
- Passwords must be stored using BCrypt.
- Never store plain text passwords.
- Never modify executed Flyway migrations.
- Every schema change must create a new migration.

---

## JPA Rules

- Prefer FetchType.LAZY.
- Avoid CascadeType.ALL.
- Use orphanRemoval only when required.
- Use constructor injection.
- Every entity should have a no-args constructor with protected access.

---

## Data Integrity Rules

- Booking history must never be deleted.
- Payment history must never be deleted.
- Use logical status instead of physical deletion whenever possible.
- Every unique business identifier must have a UNIQUE constraint.

---

# 3. Database Schema

```
Authentication

User
├── UserRole
│      └── Role
├── RefreshToken
├── EmailVerification
└── PasswordReset

----------------------------------------------------

Event

Category
      │
      ▼
Event
      │
      ▼
EventSession
      │
      ├── Venue
      │       │
      │       └── Seat
      │
      ├── SeatHold
      │
      └── Booking

----------------------------------------------------

Booking

Booking
├── BookingItem
├── Payment
│      └── PaymentTransaction
└── User
```

---

# 4. Authentication Module

The Authentication module manages user accounts, authorization, registration, login, refresh tokens, email verification and password reset.

Tables

```
users
role
user_role
refresh_token
email_verification
password_reset
```

---

# 4.1 users

## Purpose

Stores all registered users.

A User is created only after successful OTP verification.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| full_name | User full name |
| email | Login email (Unique) |
| password | BCrypt password |
| phone | Phone number |
| avatar | Avatar URL |
| status | Account status |
| email_verified | Email verification status |
| created_at | Created timestamp |
| updated_at | Updated timestamp |

---

## Relationships

User

```
1 ---- * Booking
```

User

```
1 ---- * RefreshToken
```

User

```
1 ---- * SeatHold
```

User

```
* ---- * Role
```

through UserRole.

---

## Business Rules

- Email must be unique.
- Password is stored using BCrypt.
- User is created only after OTP verification.
- Default role is CUSTOMER.
- Email verification cannot be bypassed.
- Account status is managed by UserStatus enum.

---

# 4.2 role

## Purpose

Stores all system roles.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| name | Role name (Unique) |

---

## Default Data

```
ADMIN
CUSTOMER
```

---

## Relationships

Role

```
1 ---- * UserRole
```

---

## Business Rules

- Role name must be unique.
- Roles are assigned through UserRole.
- A user may have multiple roles.

---

# 4.3 user_role

## Purpose

Many-to-many mapping table between User and Role.

---

## Columns

| Column | Description |
|----------|-------------|
| user_id | FK → user |
| role_id | FK → role |

---

## Relationships

User

```
1 ---- * UserRole
```

Role

```
1 ---- * UserRole
```

---

## Business Rules

- Composite primary key.
- One user may own multiple roles.
- One role may belong to multiple users.

---

# 4.4 refresh_token

## Purpose

Stores Refresh Tokens used to obtain new JWT Access Tokens.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| token | Refresh token (Unique) |
| revoked | Revoked flag |
| created_at | Creation time |
| expired_at | Expiration time |
| user_id | FK → user |

---

## Relationships

User

```
1 ---- * RefreshToken
```

---

## Business Rules

- One user may own multiple refresh tokens.
- Token value must be unique.
- Expired tokens cannot be reused.
- Revoked tokens cannot be reused.
- Refresh Token expires after 7 days.

---

# 4.5 email_verification

## Purpose

Temporary table for user registration before account activation.

The record exists only until OTP verification succeeds or expires.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| full_name | User full name |
| email | Registration email (Unique) |
| password | BCrypt password |
| phone | Phone number |
| otp_code | Six-digit OTP |
| expired_at | OTP expiration time |
| verified | Verification status |
| attempt_count | Number of verification attempts |
| created_at | Creation time |
| updated_at | Updated time |

---

## Relationships

None

This table is temporary and does not reference User.

---

## Business Rules

- Email must be unique.
- OTP consists of six digits.
- OTP expires after five minutes.
- Maximum five verification attempts.
- Delete record after successful verification.
- Delete expired records.
- Password is already BCrypt encoded before storing.

---

# 4.6 password_reset

## Purpose

Stores temporary information used during the Forgot Password flow.

The record is deleted after password reset succeeds or expires.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| email | User email |
| otp_code | Password reset OTP |
| expired_at | OTP expiration |
| verified | OTP verification status |
| attempt_count | Verification attempts |
| created_at | Creation time |
| updated_at | Updated time |

---

## Relationships

None

Temporary table.

---

## Business Rules

- One active password reset request per email.
- OTP expires after five minutes.
- Maximum five verification attempts.
- Delete record after successful password reset.
- Delete expired records.

---

# 5. Event Module

The Event module manages event information, event categories, venues, seats, and event schedules.

Tables

```
category
venue
seat
event
event_session
```

---

# 5.1 category

## Purpose

Stores event categories.

Categories are used to classify events.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| name | Category name (Unique) |
| description | Category description |
| created_at | Creation timestamp |
| updated_at | Last updated timestamp |

---

## Relationships

Category

```
1 ---- * Event
```

---

## Business Rules

- Category name must be unique.
- A category can contain multiple events.
- A category cannot be physically deleted if it is referenced by any event.
- Use logical status if soft delete is required in the future.

---

# 5.2 venue

## Purpose

Stores event venues.

A venue represents a physical location where events are held.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| name | Venue name |
| address | Venue address |
| capacity | Total seat capacity |
| created_at | Creation timestamp |
| updated_at | Last updated timestamp |

---

## Relationships

Venue

```
1 ---- * Seat
```

Venue

```
1 ---- * EventSession
```

---

## Business Rules

- One venue can host multiple event sessions.
- One venue owns many physical seats.
- Venue capacity should equal or exceed the number of active seats.
- Venue information should not be deleted if referenced by event sessions.

---

# 5.3 seat

## Purpose

Stores physical seats belonging to a venue.

Seats are permanent and are not recreated for each event session.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| section | Seat section |
| row_name | Row identifier |
| seat_number | Seat number |
| seat_type | Seat type |
| price_multiplier | Price multiplier |
| active | Seat availability |
| venue_id | FK → venue |

---

## Relationships

Venue

```
1 ---- * Seat
```

Seat

```
1 ---- * BookingItem
```

Seat

```
1 ---- * SeatHold
```

---

## Business Rules

- Seats belong permanently to one venue.
- Seat number must be unique within the same venue.

Unique Constraint

```
(venue_id, row_name, seat_number)
```

- price_multiplier must be greater than zero.
- Inactive seats cannot be booked.

---

# 5.4 event

## Purpose

Stores event information.

An event represents the main content that users can browse and book.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| title | Event title |
| description | Event description |
| poster | Poster image URL |
| duration | Duration (minutes) |
| age_limit | Minimum age |
| status | Event status |
| category_id | FK → category |
| created_at | Creation timestamp |
| updated_at | Last updated timestamp |

---

## Relationships

Category

```
1 ---- * Event
```

Event

```
1 ---- * EventSession
```

---

## Business Rules

- Every event belongs to one category.
- One event can have multiple event sessions.
- Duration is stored in minutes.
- Event status is managed by EventStatus enum.
- Booking history must not be affected when an event is cancelled.
- Event deletion should use logical status whenever possible.

---

# 5.5 event_session

## Purpose

Represents one scheduled showing of an event.

Each event may have multiple sessions at different venues and times.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| start_time | Session start time |
| end_time | Session end time |
| booking_open | Booking opening time |
| booking_close | Booking closing time |
| base_price | Base ticket price |
| status | Session status |
| event_id | FK → event |
| venue_id | FK → venue |
| created_at | Creation timestamp |
| updated_at | Last updated timestamp |

---

## Relationships

Event

```
1 ---- * EventSession
```

Venue

```
1 ---- * EventSession
```

EventSession

```
1 ---- * Booking
```

EventSession

```
1 ---- * SeatHold
```

EventSession

```
1 ---- * BookingItem
```

---

## Business Rules

- Every session belongs to one event.
- Every session is held at one venue.
- Booking is only allowed between booking_open and booking_close.
- start_time must be earlier than end_time.
- booking_open must be earlier than booking_close.
- booking_close must be earlier than start_time.
- Base ticket price must be greater than zero.
- Session status is managed by SessionStatus enum.

Unique Constraint

```
(event_id, venue_id, start_time)
```

This prevents duplicate sessions for the same event at the same venue and start time.

---

# 6. Booking & Payment Module

The Booking module manages seat reservation, booking creation, ticket information, payment processing and payment transaction history.

Tables

```
seat_hold
booking
booking_item
payment
payment_transaction
```

---

# 6.1 seat_hold

## Purpose

Temporarily reserves seats before payment is completed.

This table prevents multiple users from selecting the same seat simultaneously.

SeatHold records are temporary and automatically removed after expiration or successful payment.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| hold_token | Temporary hold identifier |
| status | Hold status |
| created_at | Creation timestamp |
| expired_at | Expiration timestamp |
| user_id | FK → user |
| event_session_id | FK → event_session |
| seat_id | FK → seat |

---

## Relationships

User

```
1 ---- * SeatHold
```

EventSession

```
1 ---- * SeatHold
```

Seat

```
1 ---- * SeatHold
```

---

## Business Rules

- A seat can only be held once within the same EventSession.
- Hold expires automatically after a configurable timeout.
- Expired holds must not block new bookings.
- Hold records are deleted after successful payment.
- Hold status is managed by SeatHoldStatus enum.

Unique Constraint

```
(event_session_id, seat_id)
```

---

# 6.2 booking

## Purpose

Stores booking information created by users.

A booking represents one purchase order for one event session.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| booking_code | Unique booking code |
| total_amount | Total payment amount |
| status | Booking status |
| created_at | Creation timestamp |
| updated_at | Last updated timestamp |
| user_id | FK → user |
| event_session_id | FK → event_session |

---

## Relationships

User

```
1 ---- * Booking
```

EventSession

```
1 ---- * Booking
```

Booking

```
1 ---- * BookingItem
```

Booking

```
1 ---- 1 Payment
```

---

## Business Rules

- One booking belongs to one user.
- One booking belongs to one event session.
- One booking contains one or more booking items.
- Booking code must be unique.
- Total amount equals the sum of all BookingItems.
- Booking history must never be physically deleted.
- Booking status is managed by BookingStatus enum.

Unique Constraint

```
booking_code
```

---

# 6.3 booking_item

## Purpose

Represents one booked seat.

Each BookingItem corresponds to exactly one seat.

BookingItem also stores ticket information that should remain unchanged even if the original event data changes later.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| qr_code | QR ticket code |
| price | Final ticket price |
| status | Ticket status |
| event_snapshot | Snapshot of ticket information |
| booking_id | FK → booking |
| seat_id | FK → seat |
| event_session_id | FK → event_session |

---

## Relationships

Booking

```
1 ---- * BookingItem
```

Seat

```
1 ---- * BookingItem
```

EventSession

```
1 ---- * BookingItem
```

---

## Business Rules

- Every BookingItem belongs to exactly one Booking.
- Every BookingItem represents one seat.
- Ticket price is calculated when booking is created.
- QR Code must be unique.
- Event snapshot stores immutable ticket information.
- Ticket status is managed by BookingItemStatus enum.

Unique Constraints

```
qr_code
```

```
(event_session_id, seat_id)
```

The second constraint guarantees that one seat can only be sold once within the same EventSession.

---

# 6.4 payment

## Purpose

Stores payment information for bookings.

One booking has exactly one payment.

Payment records are permanent and must never be deleted.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| amount | Payment amount |
| payment_method | Payment method |
| status | Payment status |
| created_at | Creation timestamp |
| updated_at | Last updated timestamp |
| booking_id | FK → booking |

---

## Relationships

Booking

```
1 ---- 1 Payment
```

Payment

```
1 ---- * PaymentTransaction
```

---

## Business Rules

- One booking has exactly one payment.
- Payment amount equals Booking.total_amount.
- Payment status is managed by PaymentStatus enum.
- Payment method is managed by PaymentMethod enum.
- Payment records must never be deleted.

Unique Constraint

```
booking_id
```

---

# 6.5 payment_transaction

## Purpose

Stores transaction history returned by payment gateways.

Each payment may have multiple transaction attempts.

---

## Columns

| Column | Description |
|----------|-------------|
| id | Primary key |
| provider | Payment gateway |
| transaction_code | Gateway transaction code |
| request_id | Gateway request identifier |
| amount | Transaction amount |
| currency | Currency |
| response_code | Gateway response code |
| response_message | Gateway response message |
| gateway_payload | Raw gateway response |
| status | Transaction status |
| transaction_time | Transaction timestamp |
| payment_id | FK → payment |

---

## Relationships

Payment

```
1 ---- * PaymentTransaction
```

---

## Business Rules

- Every transaction belongs to one Payment.
- Multiple transaction attempts are allowed.
- Transaction code must be unique.
- Gateway payload should be stored for auditing.
- Transaction status is managed by PaymentTransactionStatus enum.

Unique Constraint

```
transaction_code
```

---

# 7. Entity Relationships

## Authentication

User

```
1 ---- * RefreshToken
```

User

```
* ---- * Role
```

through UserRole.

EmailVerification

Temporary table.

No relationship.

PasswordReset

Temporary table.

No relationship.

---

## Event

Category

```
1 ---- * Event
```

Event

```
1 ---- * EventSession
```

Venue

```
1 ---- * EventSession
```

Venue

```
1 ---- * Seat
```

---

## Booking

User

```
1 ---- * Booking
```

EventSession

```
1 ---- * Booking
```

Booking

```
1 ---- * BookingItem
```

Booking

```
1 ---- 1 Payment
```

Payment

```
1 ---- * PaymentTransaction
```

EventSession

```
1 ---- * SeatHold
```

Seat

```
1 ---- * SeatHold
```

Seat

```
1 ---- * BookingItem
```

EventSession

```
1 ---- * BookingItem
```

---

# 8. Business Constraints

## Authentication

- Email must be unique.
- Passwords must be stored using BCrypt.
- User is created only after successful OTP verification.
- OTP expires after 5 minutes.
- Maximum 5 OTP verification attempts.
- Refresh Token expires after 7 days.
- One email can only have one active EmailVerification record.
- One email can only have one active PasswordReset request.

---

## Event

- Every Event belongs to one Category.
- Every EventSession belongs to one Event.
- Every EventSession belongs to one Venue.
- Seats are permanently attached to a Venue.
- Event duration is stored in minutes.
- Booking is only allowed during the booking window.
- Event and EventSession should use logical status instead of physical deletion.

---

## Booking

- One Booking belongs to one User.
- One Booking belongs to one EventSession.
- One Booking contains at least one BookingItem.
- Booking total_amount must equal the sum of BookingItems.
- One seat can only be held once for the same EventSession.
- One seat can only be booked once for the same EventSession.
- Booking history must never be deleted.

---

## Payment

- One Booking has exactly one Payment.
- One Payment may contain multiple PaymentTransactions.
- Payment amount must equal Booking.total_amount.
- Payment history must never be deleted.
- Gateway response must be stored for auditing.

---

# 9. Cascade Strategy

Recommended JPA Cascade configuration.

| Parent | Child | Cascade |
|----------|----------|----------|
| User | Booking | None |
| User | RefreshToken | REMOVE |
| Category | Event | None |
| Venue | Seat | None |
| Venue | EventSession | None |
| Event | EventSession | None |
| Booking | BookingItem | ALL |
| Booking | Payment | ALL |
| Payment | PaymentTransaction | ALL |

Notes

- Never cascade delete Booking history.
- Never cascade delete Event history.
- Never cascade delete Venue.
- Temporary tables may be deleted safely.

---

# 10. Index Strategy

## Unique Index

```
user.email

role.name

booking.booking_code

refresh_token.token

payment.booking_id

payment_transaction.transaction_code

seat_hold(event_session_id, seat_id)

booking_item(event_session_id, seat_id)

seat(venue_id, row_name, seat_number)

category.name
```

---

## Normal Index

```
booking.user_id

booking.event_session_id

booking.status

event.category_id

event.status

event_session.event_id

event_session.venue_id

seat.venue_id

seat_hold.user_id

seat_hold.expired_at

payment.payment_method

payment.status

payment_transaction.payment_id
```

---

# 11. Enum Definitions

## UserStatus

```
ACTIVE

LOCKED

INACTIVE
```

---

## EventStatus

```
DRAFT

PUBLISHED

CANCELLED

FINISHED
```

---

## SessionStatus

```
UPCOMING

BOOKING_OPEN

BOOKING_CLOSED

FINISHED

CANCELLED
```

---

## SeatHoldStatus

```
ACTIVE

EXPIRED

CANCELLED
```

---

## BookingStatus

```
PENDING

WAITING_PAYMENT

PAID

COMPLETED

CANCELLED

EXPIRED
```

---

## BookingItemStatus

```
VALID

USED

REFUNDED

CANCELLED
```

---

## PaymentStatus

```
PENDING

SUCCESS

FAILED

REFUNDED
```

---

## PaymentTransactionStatus

```
PENDING

SUCCESS

FAILED
```

---

## PaymentMethod

```
VNPAY

PAYOS

MOMO
```

---

## RoleName

```
ADMIN

CUSTOMER
```

---

# 12. AI Coding Notes

The AI must follow these rules when generating code.

## General Rules

- Follow the package structure defined in PROJECT_CONTEXT.md.
- Do not create additional tables unless explicitly required.
- Do not rename database tables or columns.
- Respect all foreign key relationships.
- Respect all unique constraints.
- Respect all business constraints.

---

## Entity Rules

- Generate one JPA Entity per table.
- All entities extend BaseEntity unless otherwise specified.
- Use `@Enumerated(EnumType.STRING)` for all enum fields.
- Use `FetchType.LAZY` for `@ManyToOne` and `@OneToMany`.
- Avoid `CascadeType.ALL` unless specified in this document.
- Use `BigDecimal` for monetary values.
- Use `LocalDateTime` for timestamps.

---

## Repository Rules

- Extend `JpaRepository`.
- Add query methods only when required by business logic.
- Prefer derived query methods over custom JPQL.
- Use `@Query` only for complex queries.

---

## Service Rules

- Apply business validation in the Service layer.
- Throw `BusinessException` for business rule violations.
- Annotate write operations with `@Transactional`.
- Never expose database entities directly through controllers.

---

## Security Rules

- Passwords must always be BCrypt encoded.
- Never expose Refresh Tokens in logs.
- Never expose internal exception messages.
- Validate ownership before allowing Booking or Payment operations.

---

## Performance Rules

- Avoid N+1 query problems.
- Use pagination for list APIs.
- Index frequently queried columns.
- Never use EAGER loading unless explicitly required.

---

## Flyway Rules

- Every schema change must create a new migration.
- Never modify an executed migration.
- Follow naming convention:

```
V1__create_user_table.sql

V2__create_event_table.sql

V3__create_booking_table.sql
```

---

## AI Goal

Generate production-ready Spring Boot code that is:

- Clean
- Readable
- Maintainable
- Consistent with PROJECT_CONTEXT.md
- Consistent with DATABASE.md
- Consistent with all User Story documents
- Ready for future feature expansion