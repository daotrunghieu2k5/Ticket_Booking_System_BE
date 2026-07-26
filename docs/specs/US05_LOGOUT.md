# User Story Specification

## US-05 — Logout

---

## 1. Objective

Implement the logout flow so that a signed-in user can invalidate the current refresh token and end the current session safely.

This user story comes after:

- US-01 Register with Email Verification
- US-02 Verify OTP
- US-03 Login
- US-04 Refresh Token

Logout is a security feature, not a business feature. Its purpose is to revoke the current session so the refresh token can no longer be used to obtain new access tokens.

---

## 2. Scope

### Included

- Logout API
- Validate logout request
- Find refresh token from the request
- Validate that the refresh token exists
- Validate that the refresh token is not expired
- Validate that the refresh token is not already revoked
- Mark refresh token as revoked
- Return success response

### Not Included

- Register
- OTP verification
- Login
- Refresh token generation
- Access token generation
- Forgot password
- Password reset
- Event management
- Booking management
- Payment management

### Optional / Only if already supported by current codebase

- Logout from a single device
- Logout from the current refresh token only
- Support for future logout-all-devices behavior

For this project, the default assumption is:

> Logout only revokes the refresh token that is sent by the client.

---

## 3. Business Flow

```text
Client
  ↓
POST /api/v1/auth/logout
  ↓
Validate request
  ↓
Find RefreshToken by token
  ↓
Record exists?
  ├─ NO → throw BusinessException
  └─ YES
       ↓
Check revoked?
  ├─ YES → throw BusinessException
  └─ NO
       ↓
Check expiration time
  ├─ expired → optionally mark as revoked or delete
  └─ valid
       ↓
Mark refresh token as revoked
  ↓
Save refresh token
  ↓
Return success response
```

---

## 4. Functional Requirements

### FR-01
Accept a refresh token from the client as logout input.

### FR-02
Validate the request payload.

### FR-03
Load the refresh token from the database.

### FR-04
Reject missing or invalid refresh tokens.

### FR-05
Reject tokens that are already revoked.

### FR-06
Reject tokens that are expired.

### FR-07
Mark the token as revoked when logout succeeds.

### FR-08
Persist the revocation state in the database.

### FR-09
Return a standard API success response.

---

## 5. Business Rules

### BR-01
Logout only affects the refresh token provided in the request.

### BR-02
A revoked refresh token must not be usable for future refresh requests.

### BR-03
If the refresh token is expired, the system should not treat it as a valid session token.

### BR-04
Logout must not create a new token.

### BR-05
Logout must not modify the user account itself.

### BR-06
Logout must not affect other refresh tokens unless future logout-all-devices support is explicitly added.

### BR-07
The response must not expose sensitive token internals.

### BR-08
If the refresh token is already revoked, logout should fail with a business error instead of silently succeeding.

---

## 6. API Contract

### Endpoint

```http
POST /api/v1/auth/logout
```

### Request

```json
{
  "refreshToken": "a-random-refresh-token-string"
}
```

### Response

```json
{
  "success": true,
  "message": "Logout successfully.",
  "data": null,
  "timestamp": "2026-07-25T10:00:00"
}
```

---

## 7. Validation Rules

### Refresh Token
- required
- must not be blank
- must be a non-empty string

If the project already uses UUID-style opaque refresh tokens, keep that format unchanged.

---

## 8. Database Impact

### Read
- `refresh_token`

### Update
- `refresh_token.revoked = true`

### Insert
- None

### Delete
- None by default

### Optional
If the project later decides to delete tokens instead of revoking them, that must be documented in the database design and implemented consistently across refresh/logout flows.

For the current project, the preferred behavior is:

> revoke the token, do not delete it

Reason:
- keeps audit history
- allows later analysis
- easier to reason about token lifecycle

---

## 9. Expected Classes

### Controller
- `AuthController`

### Service
- `AuthService`
- `AuthServiceImpl`

### DTO
- `LogoutRequest`
- `LogoutResponse` (optional; `ApiResponse<Void>` is acceptable)

### Repository
- `RefreshTokenRepository`

### Security / Utility
- `JwtService` or equivalent token utility only if needed to parse token metadata
- `CustomUserDetails` or equivalent only if the existing implementation already requires it

---

## 10. Transaction

Logout should be executed in a transaction if the refresh token state is updated in the database.

Use `@Transactional` for the service method that revokes the token.

Rollback if:
- the token cannot be found
- the token is already revoked
- token update fails

---

## 11. Exception Cases

Throw `BusinessException` or the project-specific exception type when:

- refresh token is missing
- refresh token is invalid
- refresh token is expired
- refresh token is already revoked

The API should return a clean business error instead of a low-level database or security exception.

---

## 12. Acceptance Criteria

- Client can send a refresh token to logout.
- Existing refresh token is marked as revoked.
- Revoked token cannot be reused for refresh.
- Expired token is rejected.
- Missing token is rejected.
- Response follows `ApiResponse<T>` format.
- Logout does not create a new token.

---

## 13. Coding Constraints

- Follow `PROJECT_CONTEXT.md`
- Follow `DATABASE.md`
- Keep the existing package structure
- Use constructor injection only
- Do not use field injection
- Use `ApiResponse<T>`
- Use `BusinessException`
- Do not modify unrelated modules
- Do not modify Flyway migrations unless explicitly required
- Generate production-ready code
- Keep the implementation simple and maintainable

---

## 14. Review Checklist

Before considering the implementation done, verify:

- refresh token lookup is correct
- revoked state is handled correctly
- expiration state is handled correctly
- database update is minimal and clear
- response format matches the API contract
- no unnecessary repository queries
- no sensitive token data is leaked
- code follows existing conventions
- logout only affects the current token

---

## 15. Architecture Decisions

After generating the code, always include an Architecture Review section.

For each important decision, explain:

### Decision
What decision was made?

### Why
Why was this approach chosen?

### Alternative
What is another reasonable approach?

### Trade-off
What are the pros and cons?

### Future Improvement
How could this evolve in a larger production system?

---

## 16. Knowledge Check

After implementation, explain the code as if mentoring a Java Backend Intern.

Include:

1. Which Spring features are used?
2. Which design patterns are used?
3. Why is this implementation better than a simpler approach?
4. What interview questions could be asked about logout and refresh token revocation?
5. What common mistakes do developers make when implementing logout?
6. Which parts of the code are most important to understand?

---

## 17. Suggested Implementation Notes

These are not strict requirements, but useful guidance for a clean implementation:

- Logout should usually only revoke the token that the client currently holds.
- If the refresh token is already expired, do not treat it as a successful logout by default.
- If the project later introduces "logout all devices", that should become a separate user story.
- Keep the logout service small and focused on token revocation only.
- Do not overcomplicate the flow with unnecessary token rotation logic.
