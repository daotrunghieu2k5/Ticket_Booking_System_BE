# User Story Specification

## US-04 — Refresh Token

---

## 1. Objective

Implement the refresh token flow so that a user can obtain a new access token without logging in again when the current access token expires.

This user story continues the authentication module after:

- US-01 Register with Email Verification
- US-02 Verify OTP
- US-03 Login

---

## 2. Scope

### Included

- Refresh token API
- Validate refresh token request
- Validate refresh token existence
- Validate expiration time
- Validate revoked state
- Load user from refresh token
- Generate a new access token
- Return new token response

### Not Included

- Register
- OTP verification
- Login
- Logout
- Forgot password
- Refresh token rotation, unless explicitly required by existing codebase
- User registration flow
- Email sending
- Role management

---

## 3. Business Flow

```text
Client
  ↓
POST /api/v1/auth/refresh-token
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
  ├─ expired → throw BusinessException
  └─ valid
       ↓
Load User from token
  ↓
User exists and active?
  ├─ NO → throw BusinessException
  └─ YES
       ↓
Generate new access token
  ↓
Return success response with access token
```

---

## 4. Functional Requirements

### FR-01
Accept a refresh token from the client.

### FR-02
Validate request payload.

### FR-03
Find the refresh token in the database.

### FR-04
Reject revoked tokens.

### FR-05
Reject expired tokens.

### FR-06
Load the associated user.

### FR-07
Verify that the user is still valid and active.

### FR-08
Generate a new access token.

### FR-09
Return the access token in a standard API response.

---

## 5. Business Rules

### BR-01
A refresh token must exist in the database.

### BR-02
A refresh token must not be revoked.

### BR-03
A refresh token must not be expired.

### BR-04
The access token must be newly generated on every refresh request.

### BR-05
If the associated user is not found or is not active, the refresh request must fail.

### BR-06
This user story does not create a new refresh token unless the current implementation already supports rotation.

### BR-07
The response must not expose sensitive token internals beyond what is needed by the client.

---

## 6. API Contract

### Endpoint

```http
POST /api/v1/auth/refresh-token
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
  "message": "Token refreshed successfully.",
  "data": {
    "accessToken": "new-access-token"
  },
  "timestamp": "2026-07-25T10:00:00"
}
```

---

## 7. Validation Rules

### Refresh Token

- required
- must not be blank
- must be a non-empty string

If the current codebase already uses UUID-style opaque refresh tokens, keep that format unchanged.

---

## 8. Database Impact

### Read

- `refresh_token`
- `user`

### Update

- None, unless the current implementation performs refresh token rotation

### Insert

- None, unless the current implementation performs refresh token rotation

### Delete

- None

---

## 9. Expected Classes

### Controller
- `AuthController`

### Service
- `AuthService`
- `AuthServiceImpl`

### DTO
- `RefreshTokenRequest`
- `RefreshTokenResponse` (if needed)

### Repository
- `RefreshTokenRepository`
- `UserRepository`

### Security / Utility
- `JwtService` or equivalent token generator utility
- `CustomUserDetails` or equivalent user principal model if already present

---

## 10. Transaction

This use case usually does not require a database transaction if it only reads the refresh token and generates a new access token.

Use `@Transactional` only if the current implementation updates the refresh token or performs rotation/revocation.

---

## 11. Exception Cases

Throw `BusinessException` or the project-specific exception type when:

- refresh token is missing
- refresh token is invalid
- refresh token is expired
- refresh token is revoked
- associated user is not found
- associated user is inactive or disabled

---

## 12. Acceptance Criteria

- Client can send a valid refresh token.
- System returns a new access token.
- Expired refresh token is rejected.
- Revoked refresh token is rejected.
- Missing refresh token is rejected.
- Response follows `ApiResponse<T>` format.

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
- Do not change Flyway migrations unless this user story explicitly requires it
- Generate production-ready code
- Keep implementation simple and maintainable

---

## 14. Review Checklist

Before considering the implementation done, verify:

- token lookup is correct
- expiration check is correct
- revoked state is handled
- user lookup is correct
- access token generation is correct
- response format matches the API contract
- no unnecessary repository queries
- no leaked sensitive data
- code follows existing conventions

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
4. What interview questions could be asked about refresh tokens?
5. What common mistakes do developers make when implementing refresh token flow?
6. Which parts of the code are most important to understand?
