# DATABASE.md

# Ticket Booking System Database Documentation

---

# 1. Database Overview

Database Engine

PostgreSQL

Migration Tool

Flyway

Naming Convention

- Table name: singular
- Column name: snake_case
- Primary key: id
- Foreign key: xxx_id

Audit Columns

Every business entity extends BaseEntity and contains:

- created_at
- updated_at

Primary Key

BIGSERIAL

---

# 2. Entity Relationship

Authentication

User
│
├── UserRole
│       │
│       ▼
│     Role
│
├── RefreshToken
│
└── Booking

EmailVerification

(independent temporary table)

--------------------------------------------

Event

Category
│
└── Event
        │
        ▼
 EventSession
        │
        ▼
      Seat

--------------------------------------------

Booking

Booking
│
├── BookingItem
│
└── Payment
        │
        ▼
PaymentTransaction

---

# 3. Database Tables

Total tables

14

1. user
2. role
3. user_role
4. refresh_token
5. email_verification
6. category
7. venue
8. event
9. event_session
10. seat
11. booking
12. booking_item
13. payment
14. payment_transaction

---

# 4. Table Details
4.1 user
Purpose

Stores registered users.

A user is created only after successful OTP verification.

Primary Key
id
Columns
Column	Description
id	Primary key
full_name	Full name
email	Login email
password	BCrypt password
phone	Phone number
enabled	Account status
created_at	Creation time
updated_at	Last update
Relationships
User

1 ---- * Booking

1 ---- * RefreshToken

1 ---- * UserRole
Business Rules
Email must be unique.
Password must be encrypted.
User cannot login before account creation.
Default role is CUSTOMER.
4.2 role
Purpose

Stores system roles.

Default Data
ADMIN

CUSTOMER
Relationships
Role

1 ---- * UserRole
4.3 user_role
Purpose

Many-to-many relationship between User and Role.

Relationships
User

1 ---- * UserRole

Role

1 ---- * UserRole
Business Rules

One user may have multiple roles.

(Currently only CUSTOMER is used.)

4.4 refresh_token
Purpose

Stores refresh tokens.

Relationships
User

1 ---- * RefreshToken
Business Rules
Refresh Token expires after 7 days.
One user may own multiple refresh tokens.
4.5 email_verification
Purpose

Temporary table for user registration.

A record exists only before account activation.

Relationships

None

Business Rules
Email must be unique.
OTP expires after 5 minutes.
Maximum 5 attempts.
Delete record after verification succeeds.
4.6 category
Purpose

Stores event categories.

Example

Movie

Concert

Theater
Relationships
Category

1 ---- * Event
4.7 venue
Purpose

Stores event locations.

Example

CGV Vincom

National Convention Center

My Dinh Stadium
Relationships
Venue

1 ---- * EventSession
4.8 event
Purpose

Stores event information.

Relationships
Category

1 ---- * Event

Event

1 ---- * EventSession
Business Rules

Deleting an event should not remove booking history.

Use logical status instead.

4.9 event_session
Purpose

Represents one schedule of an event.

Example

Movie

↓

10:00

↓

14:00

↓

18:00

Each schedule has different seats.

Relationships
Event

1 ---- * EventSession

Venue

1 ---- * EventSession

EventSession

1 ---- * Seat

EventSession

1 ---- * Booking
4.10 seat
Purpose

Stores seats of one event session.

Relationships
EventSession

1 ---- * Seat
Business Rules

Seat status

AVAILABLE

RESERVED

BOOKED
4.11 booking
Purpose

Stores booking information.

Relationships
User

1 ---- * Booking

Booking

1 ---- * BookingItem

Booking

1 ---- 1 Payment
Business Rules

Booking has multiple seats.

Total amount equals sum of BookingItems.

4.12 booking_item
Purpose

Represents one booked seat.

Relationships
Booking

1 ---- * BookingItem

Seat

1 ---- * BookingItem
Business Rules

Each BookingItem corresponds to exactly one seat.

4.13 payment
Purpose

Stores payment information.

Relationships
Booking

1 ---- 1 Payment

Payment

1 ---- * PaymentTransaction
Business Rules

Payment status

PENDING

SUCCESS

FAILED

REFUNDED
4.14 payment_transaction
Purpose

Stores transaction history from payment gateways.

Relationships
Payment

1 ---- * PaymentTransaction
Business Rules

One payment may have multiple transactions.

Example

Retry payment

↓

Fail

↓

Retry

↓

Success

Every transaction should keep provider response.

5. Business Constraints

Email

Unique

Role

Default CUSTOMER

OTP

6 digits

Expire 5 minutes

Refresh Token

7 days

Booking

Must contain at least one BookingItem.

Payment

Cannot exist without Booking.

BookingItem

Cannot exist without Booking.

6. Cascade Rules

Recommended

User

❌ Do not cascade delete Booking

Booking

✅ Cascade BookingItem

Payment

✅ Cascade PaymentTransaction

Event

❌ Do not cascade Booking

7. Index Recommendation

Unique

user.email

role.name

email_verification.email

Indexes

booking.user_id

event_session.event_id

event_session.venue_id

payment.booking_id

payment_transaction.payment_id
8. Future Extensions

Planned

QR Ticket
Seat Lock
Promotion
Coupon
Refund
Notification
Audit Log

These features are intentionally excluded from the current internship scope.