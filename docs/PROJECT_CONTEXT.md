# PROJECT_CONTEXT.md

# Ticket Booking System

## 1. Project Overview

### Project Name

Ticket Booking System

### Goal

Develop a modern web-based Ticket Booking System that allows users to:

- Browse available events
- View event details
- Select event sessions
- Choose seats
- Book tickets
- Make online payments
- Manage bookings

The project is developed as a portfolio project for a Java Backend Internship.

The focus is on:

- Clean architecture
- Maintainable code
- RESTful API
- Realistic business flow
- Secure authentication
- Good database design

---

# 2. Tech Stack

## Backend

- Java 21
- Spring Boot 4.1
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven
- Lombok
- Jakarta Validation

## Authentication

- JWT
- Refresh Token
- Email OTP Verification

## Frontend

- React
- Vite
- TailwindCSS

---

# 3. Project Architecture

Package structure

src/main/java/com/dthxhieu.ticketbooking

```
auth
booking
event
payment
venue

repository
├── auth
├── booking
├── event
├── payment
└── venue

common
├── constant
├── exception
├── mail
├── response
├── util
├── validation
└── mapper

config

security
```

Rules

- Organize code by business module.
- Repository is separated from business modules.
- Shared components must be placed inside common package.
- Do not create unnecessary packages.

---

# 4. Coding Convention

## General

- Keep code simple.
- Readability is more important than clever code.
- Avoid over-engineering.
- Every class should have a single responsibility.

---

## Dependency Injection

Always use constructor injection.

Never use:

```
@Autowired
```

Use

```
@RequiredArgsConstructor
```

---

## Naming Convention

### Entity

Use singular nouns.

Example

```
User
Role
Booking
Payment
```

### Repository

```
UserRepository
BookingRepository
PaymentRepository
```

### Service

```
AuthService
BookingService
```

### Service Implementation

```
AuthServiceImpl
BookingServiceImpl
```

### Controller

```
AuthController
BookingController
```

### DTO

```
RegisterRequest
LoginRequest

RegisterResponse
LoginResponse
```

---

## Entity Rules

Every entity should:

- extend BaseEntity (except join tables if unnecessary)
- use BIGSERIAL as primary key
- use LocalDateTime for timestamps
- use FetchType.LAZY by default
- avoid CascadeType.ALL unless required

---

# 5. Database Convention

Database

PostgreSQL

Migration

Flyway

Never use

```
spring.jpa.hibernate.ddl-auto=create
```

Every schema change must be implemented using Flyway migration.

Never modify an executed migration.

Always create a new migration.

Primary Key

```
BIGSERIAL
```

Foreign Key

```
xxx_id
```

Audit Columns

```
created_at

updated_at
```

Table naming

Use singular nouns.

Example

```
user

booking

payment

event_session
```

---

# 6. API Convention

Base URL

```
/api/v1
```

Response

Every API must return

```
ApiResponse<T>
```

HTTP Status

```
200 OK

201 CREATED

204 NO CONTENT

400 BAD REQUEST

401 UNAUTHORIZED

403 FORBIDDEN

404 NOT FOUND

409 CONFLICT

500 INTERNAL SERVER ERROR
```

Validation

Use Jakarta Validation.

Never manually validate request fields inside controller.

---

# 7. Authentication Flow

Registration

```
Client

↓

Submit Register Request

↓

Validate Request

↓

Check duplicated email

↓

Generate OTP

↓

Hash Password

↓

Save EmailVerification

↓

Send Email

↓

Wait for verification
```

OTP Verification

```
Client

↓

Submit OTP

↓

Validate OTP

↓

Create User

↓

Assign CUSTOMER role

↓

Delete EmailVerification

↓

Success
```

Login

```
Client

↓

Validate Email

↓

Validate Password

↓

Generate Access Token

↓

Generate Refresh Token

↓

Return Tokens
```

---

# 8. Business Rules

Authentication

- One email can only have one pending OTP.
- OTP expires after 5 minutes.
- Password must be encrypted before storing.
- User is created only after OTP verification succeeds.

Booking

- One User can create many Bookings.
- One Booking contains many BookingItems.
- One Booking belongs to one EventSession.

Event

- One Event has many EventSessions.
- One Venue has many EventSessions.

Payment

- One Booking has one Payment.
- One Payment has many PaymentTransactions.

Role

Default role after registration

```
CUSTOMER
```

---

# 9. Security Rules

Password

BCrypt

JWT Access Token

15 minutes

Refresh Token

7 days

OTP

- 6 digits
- expire after 5 minutes
- maximum 5 verification attempts

Never store plain text password.

Never expose internal exception message.

---

# 10. Error Handling

Use

```
BusinessException
```

Never throw

```
RuntimeException
```

directly inside business logic.

Handle exceptions globally using

```
@RestControllerAdvice
```

---

# 11. Git Workflow

Branch

```
main

develop

feature/*
```

Commit format

```
feat(auth):

feat(event):

feat(payment):

fix:

refactor:

docs:

test:
```

Every completed feature should have one meaningful commit.

---

# 12. Development Workflow

Every feature must follow:

```
Requirement

↓

Database

↓

Entity

↓

Repository

↓

DTO

↓

Mapper

↓

Service

↓

Controller

↓

Validation

↓

Testing

↓

Git Commit
```

Do not skip steps.

---

# 13. Current Project Status

Completed

- Database Design
- ERD
- Data Dictionary
- Flyway
- BaseEntity
- Authentication Entities
- Repository Layer
- Password Encoder

In Progress

- Register with Email OTP

Next

- Verify OTP
- Login
- Refresh Token
- Logout
- Forgot Password

---

# 14. AI Coding Instructions

Always read this file before generating code.

When implementing a feature:

- Follow the existing package structure.
- Do not change naming conventions.
- Do not introduce new frameworks.
- Do not modify completed migrations.
- Do not generate unnecessary abstraction.
- Prefer clean and readable code.
- Generate production-ready code.
- Explain important design decisions.
- Respect existing business rules.
- Keep each feature independently testable.

If information is missing, ask before generating code instead of making assumptions.