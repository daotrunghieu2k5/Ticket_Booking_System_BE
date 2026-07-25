# PROJECT_CONTEXT.md
# Ticket Booking System

## 1. Project Purpose

Ticket Booking System is a web-based ticket reservation platform that allows users to:

- browse events
- view event details
- select event sessions
- choose seats
- book tickets
- make online payments
- manage bookings

The project is built as a portfolio project for a Java Backend Internship.

Primary goals:

- clean architecture
- maintainable code
- realistic business flow
- secure authentication
- stable database design
- easy-to-read code that can be explained in interviews

---

## 2. Source of Truth

The following documents are the source of truth for the project:

- `docs/PROJECT_CONTEXT.md`
- `docs/DATABASE.md`
- `docs/TODO.md`
- `docs/specs/USxx_*.md`

Rules:

- If a detail exists in `DATABASE.md`, follow it.
- If a detail exists in a user story spec, follow it.
- If something is unclear or missing, ask before generating code.
- Do not invent features, fields, or packages that are not documented.

---

## 3. Tech Stack

### Backend
- Java 21
- Spring Boot 4.1
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven
- Lombok
- Jakarta Validation

### Authentication
- JWT Access Token
- Refresh Token
- Email OTP Verification

### Frontend
- React
- Vite
- TailwindCSS

Frontend is planned, but the current focus is backend development.

---

## 4. Current Project Status

### Completed
- Database design
- ERD
- Data Dictionary
- Flyway setup
- BaseEntity
- Authentication entities
- Authentication repositories
- Common infrastructure
- Register with email verification
- Verify OTP
- Login

### In Progress
- Refresh Token

### Next
- Logout
- Forgot Password
- Event module
- Booking module
- Payment module
- Admin module

---

## 5. Architecture

### Package Structure

Use this structure as the default project layout:

```text
src/main/java/com/dthxhieu/ticketbooking
├── auth
│   ├── controller
│   ├── dto
│   │   ├── request
│   │   └── response
│   ├── mapper
│   ├── service
│   │   ├── AuthService
│   │   ├── OtpService
│   │   └── impl
│   └── validator
│
├── booking
│   ├── controller
│   ├── dto
│   ├── mapper
│   └── service
│
├── event
│   ├── controller
│   ├── dto
│   ├── mapper
│   └── service
│
├── payment
│   ├── controller
│   ├── dto
│   ├── mapper
│   └── service
│
├── venue
│   ├── controller
│   ├── dto
│   ├── mapper
│   └── service
│
├── repository
│   ├── auth
│   ├── booking
│   ├── event
│   ├── payment
│   └── venue
│
├── entity
│   ├── auth
│   ├── booking
│   ├── event
│   ├── payment
│   └── venue
│
├── common
│   ├── constant
│   ├── exception
│   ├── mail
│   ├── response
│   ├── util
│   └── validation
│
├── config
├── security
└── TicketBookingApplication.java
```

### Architecture Rules
- Organize code by business module.
- Keep repository layer separated from controllers and services.
- Place shared code inside `common`.
- Do not create unnecessary packages.
- Do not introduce new architecture styles without discussion.

---

## 6. Coding Conventions

### General
- Keep code simple.
- Readability is more important than clever code.
- Avoid over-engineering.
- Each class should have a single responsibility.

### Dependency Injection
- Always use constructor injection.
- Never use field injection.
- Never rely on `@Autowired` for normal application code.
- Prefer `@RequiredArgsConstructor`.

### Entities
- Use singular class names.
- Entities should usually extend `BaseEntity`.
- Use `BIGSERIAL` for primary keys.
- Use `LocalDateTime` for timestamps.
- Use `FetchType.LAZY` by default.
- Avoid `CascadeType.ALL` unless it is clearly needed.

### DTOs
- Use request/response DTOs for API boundaries.
- Do not expose entities directly in controllers.
- Keep DTOs small and explicit.

### Services
- Service interfaces define behavior.
- Service implementations contain the logic.
- Put business rules in services, not controllers.
- Keep methods small and focused.

### Controllers
- Controllers should be thin.
- Controllers only handle request/response orchestration.
- Controllers must not contain business logic.

### Repositories
- Repositories only handle data access.
- Do not put business logic in repositories.
- Prefer derived query methods when possible.
- Use `@Query` only when needed.

---

## 7. Database Conventions

### Database Engine
- PostgreSQL

### Migration Tool
- Flyway

### Mandatory Rules
- Never use `spring.jpa.hibernate.ddl-auto=create`.
- Never use `ddl-auto=update` as the primary schema management strategy.
- Every schema change must be made through Flyway migration.
- Never modify an executed migration.
- If schema changes are required, create a new migration.

### Naming
- Table names use singular nouns, except `users` if needed to avoid PostgreSQL reserved-word issues.
- Column names use `snake_case`.
- Foreign keys use `xxx_id`.
- Audit columns use `created_at` and `updated_at`.

### Special Table Rules
- Join tables may omit `BaseEntity` if they do not need audit fields.
- Temporary workflow tables may exist without foreign keys if the business flow requires it.
- Keep database design aligned with `DATABASE.md`.

---

## 8. API Conventions

### Base URL
- `/api/v1`

### Standard Response
Every API must return `ApiResponse<T>`.

### HTTP Status Codes
Use standard HTTP semantics:

- `200 OK`
- `201 CREATED`
- `204 NO CONTENT`
- `400 BAD REQUEST`
- `401 UNAUTHORIZED`
- `403 FORBIDDEN`
- `404 NOT FOUND`
- `409 CONFLICT`
- `500 INTERNAL SERVER ERROR`

### Validation
- Use Jakarta Validation annotations.
- Do not manually validate request fields inside controllers.
- Keep validation messages clear and user-friendly.

### Error Handling
- Use `BusinessException` for business rule violations.
- Use `GlobalExceptionHandler` for centralized exception mapping.
- Do not expose raw internal exception messages to the client.

---

## 9. Authentication Flow

### Registration with Email OTP
1. Client submits register request.
2. Validate request.
3. Check duplicated email in `users`.
4. Generate OTP.
5. Hash password with BCrypt.
6. Save or update `email_verification`.
7. Send OTP email.
8. Wait for OTP verification.

### OTP Verification
1. Client submits OTP.
2. Validate request.
3. Check `email_verification`.
4. Check OTP expiration.
5. Check attempt count.
6. Compare OTP.
7. Create `User`.
8. Assign `CUSTOMER` role.
9. Delete `email_verification`.
10. Return success response.

### Login
1. Client submits email and password.
2. Validate email and password.
3. Verify password with BCrypt.
4. Generate access token.
5. Generate refresh token.
6. Return tokens.

### Refresh Token
1. Client submits refresh token.
2. Validate token.
3. Load refresh token from database.
4. Check revoked / expired state.
5. Load user.
6. Generate new access token.
7. Return new token response.

---

## 10. Business Rules

### Authentication
- One email can only have one pending OTP.
- OTP expires after 5 minutes.
- Maximum OTP attempts = 5.
- Password must be encrypted before storing.
- User is created only after OTP verification succeeds.
- After successful OTP verification, `emailVerified` should be true for the created user.
- After OTP verification succeeds, `email_verification` must be deleted.

### Role
- Default role after registration is `CUSTOMER`.

### Booking
- One user can create many bookings.
- One booking contains many booking items.
- One booking belongs to one event session.

### Event
- One event can have many event sessions.
- One venue can have many event sessions.

### Payment
- One booking has one payment.
- One payment can have many payment transactions.

---

## 11. Security Rules

### Password
- Store password using BCrypt only.
- Never store plain text password.

### JWT
- Access token expiry: 15 minutes.
- Refresh token expiry: 7 days.

### OTP
- OTP is 6 digits.
- OTP expires after 5 minutes.
- OTP should not be reusable after success.
- Maximum 5 verification attempts.

### General
- Never expose sensitive internal details in API responses.
- Never log secrets.
- Keep authentication endpoints public only where required.
- Protect all other endpoints with Spring Security.

---

## 12. Error Handling Rules

Use `BusinessException` for:

- email already exists
- user not found
- OTP invalid
- OTP expired
- OTP exceeded attempts
- refresh token invalid
- refresh token expired
- access denied business cases

Use `GlobalExceptionHandler` for:

- validation errors
- business errors
- unexpected server errors

Do not throw `RuntimeException` directly inside business logic.

---

## 13. Git Workflow

### Branches
- `main`
- `develop`
- `feature/*`
- `fix/*`

### Commit Format
Use meaningful conventional commits:

- `feat(auth): ...`
- `feat(event): ...`
- `feat(payment): ...`
- `fix: ...`
- `refactor: ...`
- `docs: ...`
- `test: ...`

### Rules
- Each completed feature should have one meaningful commit.
- Do not make huge commits containing unrelated changes.
- Keep commit messages specific.

---

## 14. Development Workflow

Every feature should follow this sequence:

1. Requirement
2. Database
3. Entity
4. Repository
5. DTO
6. Mapper
7. Service
8. Controller
9. Validation
10. Testing
11. Git commit

Do not skip steps unless the task explicitly says to.

---

## 15. AI Coding Instructions

When generating code:

- Read `PROJECT_CONTEXT.md` first.
- Read `DATABASE.md` first.
- Read the current user story spec first.
- Follow existing architecture and naming conventions.
- Do not change package structure without permission.
- Do not modify completed Flyway migrations.
- Do not introduce new frameworks without discussion.
- Do not create unnecessary abstraction.
- Prefer simple, readable, production-ready code.
- Keep features independently testable.
- Respect all existing business rules.
- If any requirement is unclear, ask for clarification before generating code.

### Required Output From AI
After generating code, always provide:

1. Files created or modified
2. Complete source code
3. Architecture Decisions
4. Knowledge Check
5. Self-review / possible improvements

### Architecture Decisions Format
For each important decision, explain:

- Decision
- Why
- Alternative
- Trade-off
- Future Improvement

### Knowledge Check Format
Explain the implementation as if mentoring a Java Backend Intern:

1. Which Spring features are used?
2. Which design patterns are used?
3. Why is this implementation better than the simpler approach?
4. What interview questions could be asked?
5. What common mistakes do developers make?
6. Which parts of the code are most important to understand?

---

## 16. Important Exclusions

Do not add the following unless explicitly required by a user story:

- Redis
- Kafka
- Microservices
- CQRS
- Event Sourcing
- Reactive stack
- Notifications
- Permissions table
- Ticket entity
- Extra API wrappers beyond `ApiResponse<T>`

If a feature is not in `DATABASE.md`, `TODO.md`, or a user story spec, do not invent it.

---

## 17. Current Working Principle

The current development style is:

- vibe coding with understanding
- one user story at a time
- AI generates code from specs
- human reviews and understands code
- commit only after the feature works

This project should remain simple, explainable, and consistent.
