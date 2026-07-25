package com.dthxhieu.ticket_booking_system_be.auth.service.impl;

import com.dthxhieu.ticket_booking_system_be.auth.dto.request.LoginRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RegisterRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.VerifyOtpRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.response.LoginResponse;
import com.dthxhieu.ticket_booking_system_be.auth.service.AuthService;
import com.dthxhieu.ticket_booking_system_be.auth.service.OtpService;
import com.dthxhieu.ticket_booking_system_be.common.constant.RoleConstant;
import com.dthxhieu.ticket_booking_system_be.common.email.MailService;
import com.dthxhieu.ticket_booking_system_be.common.enums.UserStatus;
import com.dthxhieu.ticket_booking_system_be.common.exception.BusinessException;
import com.dthxhieu.ticket_booking_system_be.common.exception.EmailAlreadyExistsException;
import com.dthxhieu.ticket_booking_system_be.common.exception.ResourceNotFoundException;
import com.dthxhieu.ticket_booking_system_be.entity.auth.EmailVerification;
import com.dthxhieu.ticket_booking_system_be.entity.auth.RefreshToken;
import com.dthxhieu.ticket_booking_system_be.entity.auth.Role;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.auth.UserRole;
import com.dthxhieu.ticket_booking_system_be.entity.auth.UserRoleId;
import com.dthxhieu.ticket_booking_system_be.repository.auth.EmailVerificationRepository;
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
}