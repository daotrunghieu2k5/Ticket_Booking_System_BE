# User Story Specification

## US-06 — Forgot Password

---

## 1. Objective

Implement the forgot password flow so that a user can reset their password securely after verifying ownership of the registered email address.

This user story belongs to the authentication module and continues after:

- US-01 Register with Email Verification
- US-02 Verify OTP
- US-03 Login
- US-04 Refresh Token
- US-05 Logout

---

## 2. Scope

### Included

- Forgot password request API
- Reset password API
- Validate email format
- Validate OTP
- Validate OTP expiration
- Validate verification attempt count
- Generate and send OTP email
- Update user password
- Revoke existing refresh tokens after password reset
- Delete temporary reset request record after successful reset

### Not Included

- Register
- Email verification for registration
- Login
- Refresh token generation
- Logout implementation details
- Event / booking / payment modules
- Password change for authenticated users
- Username-based login
- SMS OTP
- Social login

---

## 3. Business Flow

### 3.1 Forgot Password Request

```text
Client
  ↓
POST /api/v1/auth/forgot-password
  ↓
Validate request
  ↓
Find user by email
  ↓
User exists?
  ├─ NO → return generic success response (do not reveal whether the email exists)
  └─ YES
       ↓
Generate OTP
       ↓
Save or update password reset record
       ↓
Send OTP email
       ↓
Return success response
3.2 Reset Password
Client
  ↓
POST /api/v1/auth/reset-password
  ↓
Validate request
  ↓
Find password reset record by email
  ↓
Record exists?
  ├─ NO → throw BusinessException
  └─ YES
       ↓
Check OTP expiration
  ├─ expired → delete record and throw BusinessException
  └─ valid
       ↓
Check attempt count
  ├─ exceeded → delete record and throw BusinessException
  └─ valid
       ↓
Compare OTP
  ├─ wrong → increase attempt_count, save record, throw BusinessException
  └─ correct
       ↓
Find user by email
  ↓
User exists and active?
  ├─ NO → delete record and throw BusinessException
  └─ YES
       ↓
Hash new password
       ↓
Update user password
       ↓
Delete password reset record
       ↓
Revoke all refresh tokens of that user
       ↓
Return success response
4. Functional Requirements
FR-01

Accept a forgot password request containing an email address.

FR-02

Validate the email format.

FR-03

If the email belongs to a registered user, generate an OTP and send it by email.

FR-04

If the email does not belong to a registered user, return a generic success response without revealing whether the account exists.

FR-05

Accept a reset password request containing:

email
otp
newPassword
confirmNewPassword
FR-06

Validate the reset password request.

FR-07

Verify that the OTP exists and is still valid.

FR-08

Verify that the OTP has not exceeded the maximum number of attempts.

FR-09

Verify that the submitted OTP matches the stored OTP.

FR-10

Update the user's password after successful OTP verification.

FR-11

Revoke or delete all refresh tokens for that user after password reset.

FR-12

Delete the temporary password reset record after successful reset.

FR-13

Return standard ApiResponse<T> responses.

5. Business Rules
BR-01

A password reset request is temporary and must be stored only while the reset flow is active.

BR-02

One email can only have one pending password reset request at a time.

BR-03

OTP expires after 5 minutes.

BR-04

Maximum OTP attempts = 5.

BR-05

The OTP must be 6 digits.

BR-06

The new password must be BCrypt encoded before being saved to the user table.

BR-07

The forgot password request must not reveal whether the email exists.

BR-08

After a successful password reset, the password reset record must be deleted.

BR-09

After a successful password reset, all refresh tokens belonging to that user must be revoked or deleted so old sessions become invalid.

BR-10

If the OTP is wrong, increase attempt count and keep the record only if the attempt limit is not exceeded.

BR-11

If the OTP reaches the attempt limit, delete the record and require the user to start the flow again.

BR-12

The password reset flow must not create duplicate reset records for the same email.

6. API Contract
6.1 Forgot Password Request
Endpoint
POST /api/v1/auth/forgot-password
Request
{
  "email": "user@gmail.com"
}
Response
{
  "success": true,
  "message": "If the email exists, a verification code has been sent.",
  "data": null,
  "timestamp": "2026-07-25T10:00:00"
}
6.2 Reset Password
Endpoint
POST /api/v1/auth/reset-password
Request
{
  "email": "user@gmail.com",
  "otp": "123456",
  "newPassword": "NewPassword@123",
  "confirmNewPassword": "NewPassword@123"
}
Response
{
  "success": true,
  "message": "Password reset successfully.",
  "data": null,
  "timestamp": "2026-07-25T10:00:00"
}
7. Validation Rules
Forgot Password Request
email is required
email must be valid
Reset Password Request
email is required
email must be valid
otp is required
otp must be exactly 6 digits
newPassword is required
newPassword must satisfy the project password policy
confirmNewPassword is required
confirmNewPassword must match newPassword

If the project already has a shared strong-password validator, reuse it.

8. Database Impact
Read
user
password_reset
refresh_token
Insert
password_reset when the email exists and the reset flow starts
Update
password_reset when a new OTP is generated for an existing pending reset request
user.password when the password is successfully reset
Delete
password_reset after successful password reset
refresh_token records for the user after successful password reset, if the implementation deletes them instead of revoking them
Suggested Temporary Table

Create a new temporary table for this feature:

password_reset

Recommended fields:

id
email
otp_code
expired_at
attempt_count
created_at
updated_at

Notes:

This table should be temporary and should not create a foreign key to user.
One email should have at most one pending reset request.
9. Expected Classes
Controller
AuthController
Service
AuthService
AuthServiceImpl
DTO
ForgotPasswordRequest
ResetPasswordRequest
Repository
UserRepository
RefreshTokenRepository
PasswordResetRepository
Utility / Shared Services
OtpService
MailService
PasswordEncoder
Entity
PasswordReset
Exception Classes

Use existing exceptions if possible. Add new ones only if needed:

EmailNotFoundException
PasswordResetNotFoundException
OtpExpiredException
OtpAttemptExceededException
InvalidOtpException
PasswordMismatchException
10. Transaction
Forgot Password Request

Usually transactional if the implementation saves or updates the reset record and sends the email in one flow.

Reset Password

Must be transactional because it updates the user password, deletes the reset record, and revokes refresh tokens as one atomic operation.

Rollback on:

password update failure
reset record deletion failure
refresh token revocation failure
11. Exception Cases

Throw BusinessException or project-specific business exceptions when:

email format is invalid
reset record does not exist
OTP is expired
OTP is incorrect
OTP attempts exceed the limit
user is not found
new password and confirm password do not match
password reset record is missing during reset
refresh token revocation fails

For the forgot password request:

do not throw an exception when the email does not exist
return a generic success response instead
12. Acceptance Criteria
User can request a password reset using email.
OTP email is sent when the email exists.
Forgot password response does not reveal whether the email exists.
User can reset the password using email + OTP + new password.
OTP expiration is enforced.
OTP attempt limit is enforced.
User password is updated successfully.
User password is BCrypt encoded.
All refresh tokens for the user are revoked or deleted after password reset.
Temporary password reset record is deleted after successful reset.
All responses use ApiResponse<T>.
13. Coding Constraints
Follow PROJECT_CONTEXT.md
Follow DATABASE.md
Follow TODO.md
Keep the current package structure
Use constructor injection only
Do not use field injection
Use ApiResponse<T>
Use BusinessException
Use @Transactional only where appropriate
Do not modify unrelated modules
Do not change completed Flyway migrations
Do not introduce unnecessary abstractions
Generate production-ready code
Keep the implementation simple and maintainable
14. Review Checklist

Before considering the implementation complete, verify:

forgot-password request does not reveal account existence
OTP generation is reused from existing OTP service
password reset record is saved or updated correctly
OTP expiration check works
attempt count logic works
password is encoded only once
user password is updated correctly
refresh tokens are revoked or deleted
temporary record is removed after success
response format matches the API contract
validation messages are clear
no duplicate business logic is introduced
15. Architecture Decisions

After generating the code, always include an Architecture Review section.

For every important decision, explain:

Decision

What decision was made?

Why

Why was this approach chosen?

Alternative

What is another reasonable approach?

Trade-off

What are the pros and cons?

Future Improvement

How could this evolve in a larger production system?

16. Knowledge Check

After implementation, explain the code as if mentoring a Java Backend Intern.

Include:

Which Spring features are used?
Which design patterns are used?
Why is this implementation better than a simpler approach?
What interview questions could be asked about forgot-password flows?
What common mistakes do developers make when implementing password reset?
Which parts of the code are most important to understand?