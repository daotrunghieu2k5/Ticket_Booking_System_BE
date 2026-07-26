package com.dthxhieu.ticket_booking_system_be.auth.service.impl;

import com.dthxhieu.ticket_booking_system_be.auth.dto.request.ForgotPasswordRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.LoginRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RefreshTokenRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RegisterRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.ResetPasswordRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.VerifyOtpRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.response.LoginResponse;
import com.dthxhieu.ticket_booking_system_be.auth.dto.response.RefreshTokenResponse;
import com.dthxhieu.ticket_booking_system_be.auth.service.AuthService;
import com.dthxhieu.ticket_booking_system_be.auth.service.OtpService;
import com.dthxhieu.ticket_booking_system_be.common.constant.RoleConstant;
import com.dthxhieu.ticket_booking_system_be.common.email.MailService;
import com.dthxhieu.ticket_booking_system_be.common.enums.UserStatus;
import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.EmailAlreadyExistsException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.auth.EmailVerification;
import com.dthxhieu.ticket_booking_system_be.entity.auth.PasswordReset;
import com.dthxhieu.ticket_booking_system_be.entity.auth.RefreshToken;
import com.dthxhieu.ticket_booking_system_be.entity.auth.Role;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.auth.UserRole;
import com.dthxhieu.ticket_booking_system_be.entity.auth.UserRoleId;
import com.dthxhieu.ticket_booking_system_be.repository.auth.EmailVerificationRepository;
import com.dthxhieu.ticket_booking_system_be.repository.auth.PasswordResetRepository;
import com.dthxhieu.ticket_booking_system_be.repository.auth.RefreshTokenRepository;
import com.dthxhieu.ticket_booking_system_be.repository.auth.RoleRepository;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRepository;
import com.dthxhieu.ticket_booking_system_be.repository.auth.UserRoleRepository;
import com.dthxhieu.ticket_booking_system_be.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final OtpService otpService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    // Refresh Token TTL is injected from application.yml (milliseconds).
    // Converted to days in the login method for LocalDateTime arithmetic.
    @Value("${spring.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Override
    public void register(RegisterRequest request) {

        // 1. Check if this email already belongs to a verified user.
        //    If so, reject immediately - duplicate account creation is not allowed.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        // 2. Upsert EmailVerification.
        //    Business rule: "One email can only have one pending OTP."
        //    If a previous unverified attempt exists (user re-registers before verifying),
        //    we delete it and start fresh so the old OTP becomes invalid.
        if (emailVerificationRepository.existsByEmail(request.getEmail())) {
            emailVerificationRepository.deleteByEmail(request.getEmail());
            // Flush is needed so the DELETE is executed before the INSERT below,
            // preventing a unique constraint violation on the email column.
            emailVerificationRepository.flush();
        }

        // 3. Generate OTP and hash password before persisting.
        //    Password is hashed at this stage so the EmailVerification record
        //    stores only the BCrypt hash - never the plain-text password.
        String otp = otpService.generateOtp();
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 4. Build and save EmailVerification.
        //    This is a temporary record that lives only until OTP verification.
        //    It holds all data needed to create the User entity in the next step (US02).
        EmailVerification verification = EmailVerification.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(hashedPassword)
                .phone(request.getPhone())
                .otpCode(otp)
                .expiredAt(otpService.generateExpiredAt())
                .attemptCount((short) 0)
                .build();

        emailVerificationRepository.save(verification);

        // 5. Send OTP email.
        //    Placed after the DB save so that if the save fails, no email is sent.
        //    Because this method is @Transactional, a mail failure will roll back the save.
        mailService.sendOtpEmail(request.getEmail(), otp);
    }

    @Override
    public void verifyOtp(VerifyOtpRequest request) {

        // 1. Load EmailVerification - throws if no pending registration found for this email.
        EmailVerification verification = emailVerificationRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("No pending registration found for this email."));

        // 2. Check OTP expiry.
        //    BR-02: OTP expires after 5 minutes.
        //    We delete the record on expiry so the user must re-register to get a fresh OTP.
        if (LocalDateTime.now().isAfter(verification.getExpiredAt())) {
            emailVerificationRepository.delete(verification);
            throw new BusinessException("OTP has expired. Please register again.");
        }

        // 3. Check attempt limit before comparing OTP.
        //    BR-03: Maximum 5 attempts.
        //    We delete on limit exceeded - same rationale as expiry: force a clean re-registration.
        //    Checking BEFORE comparing prevents a 6th guess from succeeding after 5 wrong ones.
        if (verification.getAttemptCount() >= 5) {
            emailVerificationRepository.delete(verification);
            throw new BusinessException("Maximum OTP attempts exceeded. Please register again.");
        }

        // 4. Compare OTP.
        //    On mismatch: increment attempt count and persist - do NOT delete the record.
        //    The user still has remaining attempts.
        if (!verification.getOtpCode().equals(request.getOtp())) {
            verification.setAttemptCount((short) (verification.getAttemptCount() + 1));
            emailVerificationRepository.save(verification);
            throw new BusinessException("Invalid OTP.");
        }

        // 5. Guard against a race condition where a User was somehow created
        //    (e.g., concurrent request) between register and verify.
        //    BR-05: User email must be unique.
        if (userRepository.existsByEmail(request.getEmail())) {
            emailVerificationRepository.delete(verification);
            throw new EmailAlreadyExistsException();
        }

        // 6. Load CUSTOMER role.
        //    BR-06: Default role is CUSTOMER.
        //    ResourceNotFoundException extends BusinessException - safe to throw here.
        Role customerRole = roleRepository.findByName(RoleConstant.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("CUSTOMER role not found."));

        // 7. Create User.
        //    BR-04: Password is already BCrypt encoded - never encode again.
        //    Status is ACTIVE immediately; emailVerified = true because
        //    this very step proves the email belongs to the user.
        User user = User.builder()
                .fullName(verification.getFullName())
                .email(verification.getEmail())
                .password(verification.getPassword())
                .phone(verification.getPhone())
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        User savedUser = userRepository.save(user);

        // 8. Assign CUSTOMER role via the join entity UserRole.
        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(savedUser.getId(), customerRole.getId()))
                .user(savedUser)
                .role(customerRole)
                .build();

        userRoleRepository.save(userRole);

        // 9. Delete EmailVerification.
        //    BR-07: Remove the temporary record after successful verification.
        //    The entire method is @Transactional - if any step above fails,
        //    this delete and all preceding writes are rolled back atomically.
        emailVerificationRepository.delete(verification);
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        // 1. Find user by email.
        //    BR-05 (security): Do NOT reveal whether the email exists or the password is wrong.
        //    Both cases return the same generic message to prevent user enumeration attacks.
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Email or password is incorrect."));

        // 2. Check account status.
        //    Only ACTIVE users are allowed to login.
        //    INACTIVE or LOCKED accounts receive a distinct message (Case 03 in spec section 11).
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is inactive.");
        }

        // 3. Verify password using BCrypt.
        //    passwordEncoder.matches() compares the raw input against the stored BCrypt hash.
        //    Never compare plain text, never decode BCrypt, never re-encrypt during login.
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Email or password is incorrect.");
        }

        // 4. Generate JWT Access Token.
        //    JwtService reads userRoles from the User entity - roles must be loaded at this point.
        //    The @Transactional boundary ensures the LAZY userRoles collection is accessible here.
        String accessToken = jwtService.generateAccessToken(user);

        // 5. Generate opaque Refresh Token.
        //    FR-07: Must be a random UUID string - not a JWT, contains no user data.
        //    UUID.randomUUID() provides cryptographically strong randomness via SecureRandom.
        String rawRefreshToken = UUID.randomUUID().toString();

        // 6. Persist Refresh Token.
        //    BR-09: Each successful login creates a new Refresh Token.
        //    Multiple sessions (laptop, mobile, tablet) are supported.
        //    expiredAt = now + refreshTokenExpirationMs (converted from ms to seconds for plusSeconds).
        RefreshToken refreshToken = RefreshToken.builder()
                .token(rawRefreshToken)
                .user(user)
                .expiredAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        // 7. Build and return LoginResponse.
        //    tokenType = "Bearer" follows the RFC 6750 Bearer Token standard.
        //    expiresIn = seconds until the Access Token expires (for client-side timer).
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

        // 1. Find RefreshToken by the opaque token string.
        //    BR-01: The token must exist in the database.
        //    Using a generic error message to avoid exposing token internals.
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token."));

        // 2. Reject revoked tokens.
        //    BR-02: A revoked token must never produce a new access token.
        //    Revocation is used in logout (future US) to invalidate a session immediately.
        if (refreshToken.getRevoked()) {
            throw new BusinessException("Refresh token has been revoked.");
        }

        // 3. Reject expired tokens.
        //    BR-03: An expired token means the session has lapsed; the user must log in again.
        //    We check expiry AFTER revocation so a revoked+expired token gives the revoked message.
        if (LocalDateTime.now().isAfter(refreshToken.getExpiredAt())) {
            throw new BusinessException("Refresh token has expired. Please log in again.");
        }

        // 4. Load the associated user from the stored relationship.
        //    The RefreshToken entity holds a @ManyToOne to User - no extra DB query needed.
        //    BR-05: User must still exist and be active.
        User user = refreshToken.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is inactive.");
        }

        // 5. Generate a new access token.
        //    BR-04: Every refresh request produces a fresh access token.
        //    The @Transactional(readOnly = true) boundary keeps the Hibernate session open
        //    so the LAZY userRoles collection can be accessed by JwtService.
        String newAccessToken = jwtService.generateAccessToken(user);

        // 6. Return the new access token.
        //    BR-06: No refresh token rotation in this user story.
        //    BR-07: Only the access token is returned - no sensitive internals exposed.
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }

    @Override
    public void logout(RefreshTokenRequest request) {

        // 1. Find RefreshToken by the opaque token string.
        //    FR-04: Missing or unrecognised tokens are rejected immediately.
        //    Using a generic message to avoid leaking token structure details (BR-07).
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token."));

        // 2. Reject tokens that are already revoked.
        //    BR-08: Logout on a revoked token is an error, not a silent no-op.
        //    This prevents confused client state where the user thinks they logged out
        //    but the token was never valid to begin with.
        if (refreshToken.getRevoked()) {
            throw new BusinessException("Refresh token has already been revoked.");
        }

        // 3. Reject expired tokens.
        //    FR-06: An expired token no longer represents an active session.
        //    Treating it as a successful logout would be misleading.
        //    We check expiry AFTER revocation so a revoked+expired token gives the revoked message.
        if (LocalDateTime.now().isAfter(refreshToken.getExpiredAt())) {
            throw new BusinessException("Refresh token has expired.");
        }

        // 4. Mark the token as revoked and persist.
        //    BR-01: Only the provided token is affected - other sessions are unchanged.
        //    BR-04: No new token is created.
        //    BR-05: The User entity itself is not modified.
        //    The @Transactional boundary ensures the update is committed atomically.
        //    If the save fails, the transaction rolls back and the token remains active.
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {

        // 1. Look up the user by email.
        //    BR-07: Do NOT throw if the email is not found.
        //    A generic success response is returned regardless, so that attackers
        //    cannot determine which emails are registered (user enumeration protection).
        if (!userRepository.existsByEmail(request.getEmail())) {
            return;
        }

        // 2. Upsert PasswordReset record.
        //    BR-02 / BR-12: One email can only have one pending reset at a time.
        //    If a previous reset request exists, delete it and create a fresh one
        //    so the old OTP is invalidated immediately.
        if (passwordResetRepository.existsByEmail(request.getEmail())) {
            passwordResetRepository.deleteByEmail(request.getEmail());
            // Flush before INSERT to avoid a unique constraint violation on the email column.
            passwordResetRepository.flush();
        }

        // 3. Generate OTP.
        //    Reuses OtpService to keep OTP generation centralised.
        String otp = otpService.generateOtp();

        // 4. Save PasswordReset record.
        //    BR-01: This is a temporary record; it is deleted after a successful reset.
        PasswordReset passwordReset = PasswordReset.builder()
                .email(request.getEmail())
                .otpCode(otp)
                .expiredAt(otpService.generateExpiredAt())
                .attemptCount((short) 0)
                .build();

        passwordResetRepository.save(passwordReset);

        // 5. Send the OTP email.
        //    Placed after the DB save so no email is sent if the save fails.
        mailService.sendPasswordResetOtpEmail(request.getEmail(), otp);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {

        // 1. Load the PasswordReset record.
        //    If no record exists the user has not initiated a forgot-password flow.
        PasswordReset passwordReset = passwordResetRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("No password reset request found for this email."));

        // 2. Check OTP expiry.
        //    BR-03: OTP expires after 5 minutes.
        //    The record is deleted on expiry so the user must restart the flow.
        if (LocalDateTime.now().isAfter(passwordReset.getExpiredAt())) {
            passwordResetRepository.delete(passwordReset);
            throw new BusinessException("OTP has expired. Please request a new password reset.");
        }

        // 3. Check attempt limit BEFORE comparing OTP.
        //    BR-04: Maximum 5 attempts.
        //    Checking first prevents a 6th guess from bypassing the limit.
        if (passwordReset.getAttemptCount() >= 5) {
            passwordResetRepository.delete(passwordReset);
            throw new BusinessException("Maximum OTP attempts exceeded. Please request a new password reset.");
        }

        // 4. Compare OTP.
        //    On mismatch: increment attempt count and save - do NOT delete the record.
        //    The user still has remaining attempts.
        if (!passwordReset.getOtpCode().equals(request.getOtp())) {
            passwordReset.setAttemptCount((short) (passwordReset.getAttemptCount() + 1));
            passwordResetRepository.save(passwordReset);
            throw new BusinessException("Invalid OTP.");
        }

        // 5. Load and validate the user.
        //    The record holds only an email (no FK), so we resolve the user now.
        //    BR-05 in logout spec applies here too: inactive users cannot reset passwords.
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            passwordResetRepository.delete(passwordReset);
            throw new BusinessException("Account is inactive.");
        }

        // 6. Hash the new password and update the user.
        //    BR-06: The new password must be BCrypt encoded before being stored.
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 7. Delete the PasswordReset record.
        //    BR-08: The temporary record is removed after a successful reset.
        passwordResetRepository.delete(passwordReset);

        // 8. Revoke all existing Refresh Tokens for this user.
        //    BR-09: All active sessions become invalid after a password change.
        //    We set revoked = true on each token rather than deleting rows,
        //    consistent with the revoke-over-delete policy used in logout (US-05).
        refreshTokenRepository.findAllByUser(user)
                .forEach(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }
}