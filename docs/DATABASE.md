# DATABASE.md

# Ticket Booking System Database Documentation

---

# 1. Database Overview

## Database

PostgreSQL

## ORM

Spring Data JPA / Hibernate

## Migration

Flyway

## Primary Key

All normal entities use:

BIGSERIAL

Exception:

`user_role` uses a composite primary key.

## Naming Convention

Tables:

- singular nouns
- snake_case

Examples:

```text
users
role
event
event_session
booking
booking_item

Columns:

full_name
created_at
event_session_id

Foreign keys:

xxx_id

Examples:

user_id
event_id
venue_id
booking_id
Audit Columns

Business entities normally contain:

created_at
updated_at

Temporary tables may use audit columns only when useful.

2. Database Design Principles
PostgreSQL is the source of truth.
Flyway manages all schema changes.
Never modify an executed Flyway migration.
Every schema change requires a new migration.
Foreign keys must enforce referential integrity.
Enums are stored as VARCHAR.
Monetary values use NUMERIC and map to BigDecimal.
Timestamps map to LocalDateTime.
Passwords are stored using BCrypt.
Booking and payment history must not be physically deleted.
Prefer logical status over physical deletion for historical business data.
Do not introduce unnecessary tables.
3. Database Architecture
AUTHENTICATION

Users
├── UserRole ─── Role
├── RefreshToken
├── EmailVerification
└── PasswordReset


EVENT

Category
    │
    └── Event
          │
          └── EventSession
                │
                ├── Venue
                │     │
                │     └── Seat
                │
                ├── SeatHold
                │
                └── Booking


BOOKING & PAYMENT

Users
 │
 └── Booking
       ├── BookingItem ─── Seat
       └── Payment
              │
              └── PaymentTransaction
4. Authentication Module

Tables:

users
role
user_role
refresh_token
email_verification
password_reset
4.1 users
Purpose

Stores registered users.

A user is created only after successful email OTP verification.

Columns
Column	Description
id	Primary key
full_name	User full name
email	Login email
password	BCrypt password
phone	Phone number
avatar	Avatar URL
status	UserStatus
email_verified	Email verification flag
created_at	Creation time
updated_at	Last update
Constraints
UNIQUE(email)
Relationships
User 1:N Booking
User 1:N RefreshToken
User 1:N SeatHold
User N:M Role through UserRole
Business Rules
Email must be unique.
Password must always be BCrypt encoded.
User is created only after successful OTP verification.
Default role is CUSTOMER.
email_verified must be true for a successfully registered account.
User status is represented by UserStatus.
4.2 role
Purpose

Stores system roles.

Columns
Column	Description
id	Primary key
name	RoleName
Constraints
UNIQUE(name)
Default Roles
ADMIN
CUSTOMER
Relationships
Role 1:N UserRole
4.3 user_role
Purpose

Many-to-many mapping between User and Role.

Columns
Column	Description
user_id	FK → user
role_id	FK → role
Primary Key

Composite:

(user_id, role_id)
Rules
No duplicate user-role assignment.
This is a join table.
It does not need a separate BIGSERIAL id.
It does not need business audit fields unless required by the implementation.
4.4 refresh_token
Purpose

Stores refresh tokens used to obtain new access tokens.

Columns
Column	Description
id	Primary key
token	Opaque refresh token
revoked	Revocation flag
created_at	Creation time
expired_at	Expiration time
user_id	FK → user
Constraints
UNIQUE(token)
Rules
One user may have multiple refresh tokens.
Token expires after 7 days.
Revoked tokens cannot be reused.
Expired tokens cannot be reused.
Refresh tokens must not be logged.
4.5 email_verification
Purpose

Temporary data used during registration.

Columns
Column	Description
id	Primary key
full_name	Registration full name
email	Registration email
password	BCrypt password
phone	Phone number
otp_code	Six-digit OTP
expired_at	OTP expiration
verified	Verification status
attempt_count	Number of attempts
created_at	Creation time
updated_at	Last update
Constraints
UNIQUE(email)
Rules
OTP contains exactly 6 digits.
OTP expires after 5 minutes.
Maximum 5 verification attempts.
Password is already BCrypt encoded.
Record is deleted after successful verification.
Record may be deleted when expired or locked.
This table does not reference User because User is created after verification.
4.6 password_reset
Purpose

Temporary data used during Forgot Password.

Columns
Column	Description
id	Primary key
email	User email
otp_code	Six-digit OTP
expired_at	OTP expiration
verified	Verification status
attempt_count	Number of attempts
created_at	Creation time
updated_at	Last update
Rules
One active reset request per email.
OTP contains exactly 6 digits.
OTP expires after 5 minutes.
Maximum 5 attempts.
Record is deleted after successful password reset.
Expired records may be deleted.
This table is temporary and does not need a foreign key to User.
5. Event Module

Tables:

category
venue
seat
event
event_session
5.1 category
Purpose

Stores event categories.

Columns
Column	Description
id	Primary key
name	Category name
description	Category description
created_at	Creation time
updated_at	Last update
Constraints
UNIQUE(name)

Application-level comparison should ignore surrounding spaces and case.

Relationships
Category 1:N Event
Rules
Category name cannot be blank.
Category name must be unique.
Category cannot be deleted while referenced by Event.
5.2 venue
Purpose

Stores physical event locations.

Columns
Column	Description
id	Primary key
name	Venue name
address	Venue address
capacity	Declared venue capacity
created_at	Creation time
updated_at	Last update
Relationships
Venue 1:N Seat
Venue 1:N EventSession
Rules
A venue can contain many physical seats.
A venue can host many sessions.
Venue must not be deleted while referenced by EventSession.
Capacity must be greater than zero.
Active seat count must not exceed venue capacity.
5.3 seat
Purpose

Stores permanent physical seats belonging to a Venue.

IMPORTANT:

A Seat belongs to a Venue, NOT to an EventSession.

The same physical seat can be used by many EventSessions held at the same Venue.

Columns
Column	Description
id	Primary key
section	Seat section
row_name	Row identifier
seat_number	Seat number
seat_type	Seat type
price_multiplier	Price multiplier
active	Whether the physical seat is active
venue_id	FK → venue
Constraints
UNIQUE(venue_id, row_name, seat_number)
Relationships
Venue 1:N Seat
Seat 1:N SeatHold
Seat 1:N BookingItem
Rules
Seat permanently belongs to one Venue.
Seat cannot belong to multiple Venues.
Inactive seats cannot be booked.
price_multiplier > 0.
Seat identity is determined by:
venue_id + row_name + seat_number

IMPORTANT:

Do NOT add:

AVAILABLE
RESERVED
BOOKED

as a permanent Seat status.

Seat availability depends on the EventSession.

5.4 event
Purpose

Stores the main event information.

Columns
Column	Description
id	Primary key
title	Event title
description	Event description
poster	Poster URL
duration	Duration in minutes
age_limit	Minimum age
status	EventStatus
category_id	FK → category
created_at	Creation time
updated_at	Last update
Relationships
Category 1:N Event
Event 1:N EventSession
Rules
Every Event belongs to one Category.
An Event can have multiple EventSessions.
Duration is stored in minutes.
Event uses EventStatus.
Event cancellation must not delete Booking history.
Prefer logical status instead of physical deletion.
5.5 event_session
Purpose

Represents one scheduled showing of an Event.

Columns
Column	Description
id	Primary key
start_time	Session start time
end_time	Session end time
booking_open	Booking opening time
booking_close	Booking closing time
base_price	Base ticket price
status	SessionStatus
event_id	FK → event
venue_id	FK → venue
created_at	Creation time
updated_at	Last update
Constraints
UNIQUE(event_id, venue_id, start_time)
Relationships
Event 1:N EventSession
Venue 1:N EventSession
EventSession 1:N SeatHold
EventSession 1:N Booking
Rules
Every session belongs to one Event.
Every session belongs to one Venue.
start_time < end_time.
booking_open < booking_close.
booking_close <= start_time.
base_price > 0.
Booking is allowed only during the booking window.
Session status uses SessionStatus.
Session cancellation must not delete historical Booking data.

IMPORTANT:

Creating an EventSession does NOT create new Seat records.

The Session reuses the physical Seats belonging to its Venue.

6. Booking & Payment Module

Tables:

seat_hold
booking
booking_item
payment
payment_transaction
6.1 seat_hold
Purpose

Temporarily holds a physical seat for a specific EventSession.

Columns
Column	Description
id	Primary key
hold_token	Temporary hold identifier
status	SeatHoldStatus
created_at	Creation time
expired_at	Expiration time
user_id	FK → user
event_session_id	FK → event_session
seat_id	FK → seat
Constraints
UNIQUE(event_session_id, seat_id)
Rules
A seat can have at most one active hold for the same EventSession.
Expired holds must not block new bookings.
Hold belongs to both a Session and a Seat.
The selected Seat must belong to the Venue of the EventSession.
Hold status uses SeatHoldStatus.
Hold may be deleted after successful payment.

IMPORTANT:

SeatHold determines temporary reservation state.

It does NOT change the permanent Seat record.

6.2 booking
Purpose

Stores a user's booking for one EventSession.

Columns
Column	Description
id	Primary key
booking_code	Unique booking identifier
total_amount	Total booking amount
status	BookingStatus
created_at	Creation time
updated_at	Last update
user_id	FK → user
event_session_id	FK → event_session
Constraints
UNIQUE(booking_code)
Relationships
User 1:N Booking
EventSession 1:N Booking
Booking 1:N BookingItem
Booking 1:1 Payment
Rules
Booking belongs to exactly one User.
Booking belongs to exactly one EventSession.
Booking must contain at least one BookingItem.
total_amount equals the sum of BookingItem prices.
Booking history must never be physically deleted.
Booking status uses BookingStatus.
6.3 booking_item
Purpose

Represents one purchased seat.

Columns
Column	Description
id	Primary key
qr_code	QR ticket code
price	Final ticket price
status	BookingItemStatus
event_snapshot	Immutable event/ticket snapshot
booking_id	FK → booking
seat_id	FK → seat
event_session_id	FK → event_session
Constraints
UNIQUE(qr_code)

UNIQUE(event_session_id, seat_id)
Important Consistency Rule
booking_item.event_session_id
=
booking.event_session_id

The Service layer MUST guarantee this invariant.

Relationships
Booking 1:N BookingItem
Seat 1:N BookingItem
EventSession 1:N BookingItem
Rules
One BookingItem represents exactly one Seat.
One Seat can only be sold once in one EventSession.
Ticket price is fixed when the BookingItem is created.
QR code must be unique.
Event snapshot is immutable.
BookingItem status uses BookingItemStatus.
6.4 payment
Purpose

Stores payment information for a Booking.

Columns
Column	Description
id	Primary key
amount	Payment amount
payment_method	PaymentMethod
status	PaymentStatus
created_at	Creation time
updated_at	Last update
booking_id	FK → booking
Constraints
UNIQUE(booking_id)
Rules
One Booking has exactly one Payment.
Payment amount must equal Booking.total_amount.
Payment status uses PaymentStatus.
Payment method uses PaymentMethod.
Payment history must never be physically deleted.
6.5 payment_transaction
Purpose

Stores every payment gateway transaction attempt.

Columns
Column	Description
id	Primary key
provider	Payment provider
transaction_code	Gateway transaction code
request_id	Gateway request ID
amount	Transaction amount
currency	Currency
response_code	Gateway response code
response_message	Gateway response message
gateway_payload	Raw gateway response
status	PaymentTransactionStatus
transaction_time	Gateway transaction time
payment_id	FK → payment
Constraints
UNIQUE(transaction_code)
Relationships
Payment 1:N PaymentTransaction
Rules
One Payment may have multiple transaction attempts.
Transaction history must never be deleted.
Gateway response should be retained for auditing.
Transaction status uses PaymentTransactionStatus.
7. Entity Relationships
Authentication
Users 1:N RefreshToken

Users N:M Role
through UserRole

Users 1:N Booking

Users 1:N SeatHold

Temporary authentication tables:

EmailVerification
PasswordReset

do not require foreign keys to Users.

Event
Category 1:N Event

Event 1:N EventSession

Venue 1:N EventSession

Venue 1:N Seat
Booking
EventSession 1:N SeatHold

Seat 1:N SeatHold

Users 1:N SeatHold

Users 1:N Booking

EventSession 1:N Booking

Booking 1:N BookingItem

Seat 1:N BookingItem

EventSession 1:N BookingItem

Booking 1:1 Payment

Payment 1:N PaymentTransaction
8. Seat Availability Model

This section is critical for AI implementation.

A Seat is a permanent physical resource of a Venue.

Seat availability is NOT stored directly on Seat.

For a specific EventSession:

AVAILABLE

means:

No active SeatHold
AND
No successful BookingItem
RESERVED

means:

An active SeatHold exists
BOOKED

means:

A valid BookingItem exists

Conceptually:

Venue
  │
  └── Seat A1
        │
        ├── Session 1 → AVAILABLE
        ├── Session 2 → RESERVED
        └── Session 3 → BOOKED

Therefore:

Never create separate Seat records for every EventSession.
Never store session-specific seat status in the Seat table.
Always query Seat together with EventSession when determining availability.
When creating a Session, reuse the Seats belonging to its Venue.
9. Business Constraints
Authentication
users.email UNIQUE

role.name UNIQUE

refresh_token.token UNIQUE

email_verification.email UNIQUE

password_reset
one active request per email
Event
category.name UNIQUE

seat(venue_id, row_name, seat_number) UNIQUE

event_session(event_id, venue_id, start_time) UNIQUE

Rules:

Event must belong to Category.
EventSession must belong to Event.
EventSession must belong to Venue.
Seat must belong to Venue.
EventSession can only use Seats belonging to its Venue.
Booking
booking.booking_code UNIQUE

booking_item.qr_code UNIQUE

booking_item(event_session_id, seat_id) UNIQUE

Rules:

One seat can only be sold once per EventSession.
One seat can only have one active hold per EventSession.
Booking must contain at least one BookingItem.
Booking total must equal BookingItem total.
Payment
payment.booking_id UNIQUE

payment_transaction.transaction_code UNIQUE
10. Cascade Strategy

Use cascade carefully.

Parent	Child	Recommended
User	Booking	NONE
User	RefreshToken	REMOVE
Booking	BookingItem	ALL
Booking	Payment	ALL
Payment	PaymentTransaction	ALL
Venue	Seat	NONE
Venue	EventSession	NONE
Event	EventSession	NONE
Category	Event	NONE

Important:

Never cascade delete Booking history.
Never cascade delete Payment history.
Never cascade delete Event history.
Never cascade delete Venue.
Do not use CascadeType.ALL by default.
Temporary authentication records may be physically deleted.
11. Index Strategy
Unique Indexes / Constraints
users.email

role.name

refresh_token.token

category.name

booking.booking_code

booking_item.qr_code

payment.booking_id

payment_transaction.transaction_code

seat(venue_id, row_name, seat_number)

event_session(event_id, venue_id, start_time)

seat_hold(event_session_id, seat_id)

booking_item(event_session_id, seat_id)
Normal Indexes
booking.user_id

booking.event_session_id

booking.status

event.category_id

event.status

event_session.event_id

event_session.venue_id

event_session.status

seat.venue_id

seat_hold.user_id

seat_hold.event_session_id

seat_hold.expired_at

booking_item.booking_id

booking_item.seat_id

payment.status

payment_transaction.payment_id
12. Enum Definitions
UserStatus
ACTIVE
LOCKED
INACTIVE
EventStatus
DRAFT
PUBLISHED
CANCELLED
FINISHED
SessionStatus
UPCOMING
BOOKING_OPEN
BOOKING_CLOSED
FINISHED
CANCELLED
SeatHoldStatus
ACTIVE
EXPIRED
CANCELLED
BookingStatus
PENDING
WAITING_PAYMENT
PAID
COMPLETED
CANCELLED
EXPIRED
BookingItemStatus
VALID
USED
REFUNDED
CANCELLED
PaymentStatus
PENDING
SUCCESS
FAILED
REFUNDED
PaymentTransactionStatus
PENDING
SUCCESS
FAILED
PaymentMethod
VNPAY
PAYOS
MOMO
RoleName
ADMIN
CUSTOMER
13. AI Coding Notes

The AI MUST read this document before generating database-related code.

Entity
One JPA Entity per database table.
user_role uses a composite key.
Normal entities use BIGSERIAL.
Use @Enumerated(EnumType.STRING) for enums.
Use BigDecimal for monetary values.
Use LocalDateTime for timestamps.
Use LAZY relationships by default.
Do not expose entities directly through REST APIs.
Seat

IMPORTANT:

Seat belongs to Venue.
Seat does NOT belong to EventSession.

Do NOT create:

event_session.seat_id

Do NOT create a new Seat whenever an EventSession is created.

Session-specific availability must be calculated using:

SeatHold
BookingItem
EventSession

When creating an EventSession:

Validate Event.
Validate Venue.
Validate schedule.
Save EventSession.
Seats are NOT copied or recreated.

The Session automatically uses the physical Seats of its Venue.

Booking

When creating BookingItem:

Validate EventSession.
Validate Seat.
Ensure Seat belongs to EventSession.venue.
Ensure Seat is not actively held.
Ensure Seat is not already booked.
Create BookingItem.
Preserve event_session_id.
Preserve ticket price.
BookingItem Consistency

The following must always be true:

booking_item.booking.event_session_id
==
booking_item.event_session_id

The Service layer is responsible for maintaining this invariant.

SeatHold

Before creating a SeatHold:

Seat must belong to EventSession.venue

Then ensure:

No active hold exists
AND
No valid booking exists

Expired holds must not prevent a new hold.

Repository
Extend JpaRepository.
Prefer derived query methods.
Add custom queries only when necessary.
Use existsBy... for existence checks.
Avoid loading entire collections when only existence is required.
Service
Business rules belong in Service.
Controllers must remain thin.
Use @Transactional for write operations.
Use BusinessException for business rule violations.
Do not throw RuntimeException directly.
Flyway

Never modify executed migrations.

Always create a new migration:

Vxx__description.sql

Example:

V15__create_seat_hold.sql

The actual version must follow the project's current migration version.

Security
Passwords must use BCrypt.
Refresh tokens must not be logged.
Booking operations must validate ownership.
Admin operations require ADMIN role.
Performance
Avoid N+1 queries.
Use pagination for large list APIs.
Use indexes defined in this document.
Prefer existence queries when checking constraints.
Do not use EAGER loading without a specific reason.
14. Critical Design Decisions
Decision 1 — Seat belongs to Venue, not EventSession
Why

A physical venue owns a fixed set of seats.

Multiple EventSessions can reuse those same physical seats.

Alternative

Create separate Seat records for every EventSession.

Trade-off

The current design is more normalized and avoids duplicating physical seat definitions.

Session-specific state is handled separately by SeatHold and BookingItem.

Decision 2 — Do not store AVAILABLE / RESERVED / BOOKED in Seat
Why

Seat availability changes depending on the EventSession.

Alternative

Store status directly in Seat.

Trade-off

A direct status would be incorrect because the same Seat may be BOOKED in one Session and AVAILABLE in another.

Decision 3 — Keep event_session_id in BookingItem
Why

It allows:

UNIQUE(event_session_id, seat_id)

which protects against selling the same seat twice for one Session.

Alternative

Derive EventSession only through Booking.

Trade-off

The field is technically duplicated because Booking already references EventSession.

Therefore the Service layer must guarantee:

booking_item.event_session_id
=
booking.event_session_id
Decision 4 — Keep SeatHold as a separate table
Why

Temporary reservation state has a different lifecycle from the permanent Seat entity.

Alternative

Add reservation fields directly to Seat.

Trade-off

That would make one physical Seat unable to represent different reservation states across different EventSessions.

15. Final Database Model

The final model is:

USERS
 ├── USER_ROLE ─── ROLE
 ├── REFRESH_TOKEN
 ├── BOOKING
 └── SEAT_HOLD

EMAIL_VERIFICATION
PASSWORD_RESET


CATEGORY
    │
    └── EVENT
          │
          └── EVENT_SESSION
                │
                ├── VENUE
                │     │
                │     └── SEAT
                │
                ├── SEAT_HOLD
                │
                └── BOOKING
                       │
                       ├── BOOKING_ITEM ─── SEAT
                       │
                       └── PAYMENT
                              │
                              └── PAYMENT_TRANSACTION

This is the database model that all future User Stories must follow.

AI must NOT introduce another Seat configuration table unless the project requirements explicitly change.