package com.dthxhieu.ticket_booking_system_be.auth.service;

import com.dthxhieu.ticket_booking_system_be.auth.dto.request.ForgotPasswordRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.LoginRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RefreshTokenRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RegisterRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.ResetPasswordRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.VerifyOtpRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.response.LoginResponse;
import com.dthxhieu.ticket_booking_system_be.auth.dto.response.RefreshTokenResponse;

public interface AuthService {

    // Register creates an EmailVerification record and sends an OTP email.
    // A real User is NOT created here - only after OTP verification succeeds.
    void register(RegisterRequest request);

    // Verifies the OTP, then creates the User and assigns the CUSTOMER role.
    void verifyOtp(VerifyOtpRequest request);

    // Authenticates the user, generates JWT + Refresh Token, stores the Refresh Token.
    LoginResponse login(LoginRequest request);

    // Validates the existing Refresh Token and issues a new Access Token.
    // Does NOT create a new Refresh Token (no rotation in this user story).
    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    // Revokes the provided Refresh Token to end the current session.
    // Only the supplied token is affected; other active sessions remain valid.
    void logout(RefreshTokenRequest request);

    // Generates a password reset OTP and sends it to the provided email.
    // Does NOT reveal whether the email belongs to a registered user (BR-07).
    void forgotPassword(ForgotPasswordRequest request);

    // Validates the OTP and updates the user's password.
    // Revokes all existing refresh tokens for the user after a successful reset.
    void resetPassword(ResetPasswordRequest request);

}
