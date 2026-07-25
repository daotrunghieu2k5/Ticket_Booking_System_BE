# User Story Specification

## US-03 — User Login

---

# 1. Objective

Implement the user login feature.

A registered and verified user can authenticate into the system using email and password.

After successful authentication:

- Generate JWT Access Token.
- Generate Refresh Token.
- Store Refresh Token.
- Return authentication information to client.

This feature allows users to access protected resources after authentication.

---

# 2. Scope

This user story includes:

- Login API.
- Validate login request.
- Find user by email.
- Verify password using BCrypt.
- Check user account status.
- Generate JWT Access Token.
- Generate Refresh Token.
- Store Refresh Token.
- Return authentication response.


This user story does NOT include:

- User registration.
- Email OTP verification.
- Forgot Password.
- Reset Password.
- Google OAuth Login.
- Refresh Token API.
- Logout.
- Account lock mechanism.
- Role permission management.

---

# 3. Business Flow

```text
Client

↓

POST /api/v1/auth/login

↓

Validate Request

↓

Find User By Email

↓

User exists?

NO
↓

Throw BusinessException

YES
↓

User status active?

NO
↓

Throw BusinessException

YES
↓

Verify Password Using BCrypt

↓

Password correct?

NO
↓

Throw BusinessException

YES
↓

Generate JWT Access Token

↓

Generate Opaque Refresh Token

↓

Save Refresh Token

↓

Return Login Response
4. Functional Requirements
FR-01

Receive login information:

email
password
FR-02

Validate login request.

FR-03

Find user by email.

FR-04

Verify user account status.

Only active and verified users can login.

FR-05

Verify password.

Password comparison must use BCrypt.

Example:

passwordEncoder.matches(
    rawPassword,
    encodedPassword
)
FR-06

Generate JWT Access Token.

The JWT payload should contain:

userId
email
roles

JWT is used for accessing protected APIs.

FR-07

Generate Refresh Token.

Refresh Token must:

Be a random unique UUID string.
Be an opaque token.
Not contain user information.
Not be a JWT.

Example:

8f3d7c92-7e54-4c4a-a4c1-2f8f9f2d8abc
FR-08

Store Refresh Token into database.

Stored information:

token
user_id
expiration_time
created_at
revoked_status
FR-09

Return authentication response.

Response includes:

Access Token.
Refresh Token.
Token type.
Expiration time.

User information is NOT required because frontend can decode JWT Access Token to retrieve:

userId
email
roles

Optional future improvement:

Return basic user information:

{
    "id": 1,
    "email": "customer@gmail.com",
    "roles": [
        "CUSTOMER"
    ]
}
5. Business Rules
BR-01

Only verified users can login.

BR-02

Email must exist in database.

BR-03

Email must be unique.

BR-04

Password must be verified using BCrypt.

Never:

Compare plain password.
Decode BCrypt manually.
Re-encrypt password during login.
BR-05

System must not reveal sensitive authentication information.

Invalid login response:

Email or password is incorrect.

Do not return:

Email does not exist.

or

Wrong password.
BR-06

Access Token expiration:

15 minutes
BR-07

Refresh Token expiration:

7 days
BR-08

Refresh Token must be unique.

Each generated token belongs to one user session.

BR-09

Each successful login creates a new Refresh Token.

Multiple active sessions are allowed:

Example:

Laptop login
+
Mobile login
+
Tablet login
BR-10 (Optional)

Limit active Refresh Tokens per user.

Example:

Maximum active sessions: 5

When the limit is exceeded:

Delete oldest Refresh Token.
Keep latest active sessions.

This rule is optional and can be implemented in future versions.

BR-11

Future security improvement:

Implement brute force protection.

Example:

After multiple failed login attempts:

5 failed attempts
        ↓
Temporary account lock
        ↓
Unlock after timeout

This feature is NOT included in US-03.

6. API Contract
Endpoint
POST /api/v1/auth/login
Request
{
    "email": "customer@gmail.com",
    "password": "123456"
}
Success Response
{
    "success": true,
    "message": "Login successfully.",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "8f3d7c92-7e54-4c4a-a4c1-2f8f9f2d8abc",
        "tokenType": "Bearer",
        "expiresIn": 900
    },
    "timestamp": "2026-07-25T10:00:00"
}
Failed Response
{
    "success": false,
    "message": "Email or password is incorrect.",
    "data": null,
    "timestamp": "2026-07-25T10:00:00"
}
7. Validation Rules
Email

Required.

Must follow valid email format.

Example:

customer@gmail.com
Password

Required.

Minimum:

6 characters
8. Database Impact
Read
users

roles

user_roles
Insert
refresh_tokens
9. Expected Classes
Controller
AuthController
Service
AuthService

AuthServiceImpl
DTO

Request:

LoginRequest

Response:

LoginResponse
Repository
UserRepository

RefreshTokenRepository

RoleRepository
Security
JwtService

PasswordEncoder

SecurityConfig
10. Transaction

The Refresh Token creation process must execute inside one transaction.

Rollback when:

Refresh Token generation fails.
Refresh Token saving fails.

Example:

@Transactional
public LoginResponse login(LoginRequest request)
11. Exception Cases

Return BusinessException when:

Case 01

User email does not exist.

Message:

Email or password is incorrect.
Case 02

Password incorrect.

Message:

Email or password is incorrect.
Case 03

User account inactive.

Message:

Account is inactive.
Case 04

Refresh Token creation failed.

Message:

Unable to create refresh token.
Case 05

JWT generation failed.

Message:

Authentication failed.
12. Acceptance Criteria

✔ Verified user can login successfully.

✔ Invalid credentials are rejected.

✔ Password is checked using BCrypt.

✔ JWT Access Token is generated.

✔ Refresh Token is generated as UUID opaque token.

✔ Refresh Token is stored securely.

✔ Authentication response follows ApiResponse format.

✔ No sensitive authentication information is exposed.

✔ Multiple sessions are supported.

✔ Transaction rollback works correctly.

13. Coding Constraints

Follow:

PROJECT_CONTEXT.md

DATABASE.md

Use:

Constructor injection.
Jakarta Validation.
@Transactional.
ApiResponse.
BusinessException.

Never:

Use RuntimeException directly.
Store plain password.
Return password.
Generate JWT inside Controller.
Put business logic inside Controller.
Use Refresh Token as JWT.
Modify unrelated modules.
Change package structure.

Generate production-ready code.

14. Future Improvements

The following features can be implemented in future User Stories:

Refresh Token Management
Refresh Token API.
Token rotation.
Token revoke.
Logout all devices.
Security Enhancement
Brute force protection.
Account temporary lock.
Login history.
User Experience
Return user profile information.
Remember device.
Session management.
15. Review Checklist

Before finishing, verify:

Login flow follows authentication architecture.
Refresh Token is UUID opaque token.
JWT and Refresh Token responsibilities are separated.
Password verification uses BCrypt.
No sensitive information leaked.
Refresh Token persistence is correct.
Transaction boundary is correct.
Repository methods are minimal.
Validation is implemented.
Exception handling is consistent.
No duplicated authentication logic.
No dead code.
No TODO comments.
Clean code.