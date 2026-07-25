# User Story Specification

## US-02 — Verify Email OTP

---

# 1. Objective

Implement the email verification feature.

A user account must only be created after successful OTP verification.

This feature completes the registration flow started in US-01.

---

# 2. Scope

This user story includes:

- Verify OTP API
- Validate OTP
- Validate expiration time
- Validate attempt count
- Create User
- Assign CUSTOMER role
- Remove EmailVerification record

This user story does NOT include:

- Login
- Refresh Token
- Forgot Password
- Email sending
- JWT generation

---

# 3. Business Flow

```text
Client

↓

POST /api/v1/auth/verify-otp

↓

Validate Request

↓

Find EmailVerification

↓

Record exists?

NO
↓

Throw BusinessException

YES
↓

OTP expired?

YES
↓

Delete record

↓

Throw BusinessException

NO
↓

Attempt count >= 5 ?

YES
↓

Delete record

↓

Throw BusinessException

NO
↓

OTP correct?

NO
↓

Increase attempt_count

↓

Save

↓

Throw BusinessException

YES
↓

User already exists?

YES
↓

Throw BusinessException

NO
↓

Load CUSTOMER role

↓

Create User

↓

Create UserRole

↓

Delete EmailVerification

↓

Return Success
```

---

# 4. Functional Requirements

## FR-01

Receive

```
email
otp
```

---

## FR-02

Validate request.

---

## FR-03

Load EmailVerification.

---

## FR-04

Validate

- expiration
- attempts

---

## FR-05

Compare OTP.

---

## FR-06

Create User.

---

## FR-07

Assign CUSTOMER role.

---

## FR-08

Delete EmailVerification.

---

## FR-09

Return success response.

---

# 5. Business Rules

BR-01

EmailVerification must exist.

---

BR-02

OTP expires after 5 minutes.

---

BR-03

Maximum attempts

```
5
```

---

BR-04

Password is already BCrypt encoded.

Never encode again.

---

BR-05

User email must be unique.

---

BR-06

Default role

```
CUSTOMER
```

---

BR-07

Delete EmailVerification after successful verification.

---

BR-08

Delete EmailVerification after maximum attempts exceeded.

---

# 6. API Contract

## Endpoint

```
POST /api/v1/auth/verify-otp
```

---

Request

```json
{
    "email": "abc@gmail.com",
    "otp": "123456"
}
```

---

Response

```json
{
    "success": true,
    "message": "Account verified successfully.",
    "data": null,
    "timestamp": "2026-07-25T10:00:00"
}
```

---

# 7. Validation Rules

Email

- required
- valid format

OTP

- required
- exactly 6 digits

---

# 8. Database Impact

Read

- email_verification
- role

Insert

- user
- user_role

Delete

- email_verification

---

# 9. Expected Classes

Controller

- AuthController

Service

- AuthService
- AuthServiceImpl

DTO

- VerifyOtpRequest

Repository

- EmailVerificationRepository
- UserRepository
- RoleRepository
- UserRoleRepository

---

# 10. Transaction

The entire verification flow must execute within one transaction.

Rollback when:

- User creation fails
- Role assignment fails
- Delete EmailVerification fails

---

# 11. Exception Cases

Return BusinessException when:

- Email not found
- OTP expired
- OTP incorrect
- OTP exceeded attempts
- User already exists
- CUSTOMER role not found

---

# 12. Acceptance Criteria

✔ User created.

✔ CUSTOMER role assigned.

✔ EmailVerification removed.

✔ ApiResponse<Void> returned.

✔ No duplicated user.

✔ Password remains BCrypt encoded.

---

# 13. Coding Constraints

Follow PROJECT_CONTEXT.md.

Follow DATABASE.md.

Use constructor injection.

Use @Transactional.

Use ApiResponse.

Use BusinessException.

Never use RuntimeException directly.

Never modify unrelated modules.

Never change package structure.

Generate production-ready code.

---

# 14. Review Checklist

Before finishing, verify:

- No duplicated SQL
- No duplicated business logic
- Transaction is correct
- Repository methods are minimal
- Validation uses Jakarta Validation
- Method names are meaningful
- No dead code
- No TODO comments
- Clean code