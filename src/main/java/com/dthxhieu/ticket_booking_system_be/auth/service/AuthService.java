package com.dthxhieu.ticket_booking_system_be.auth.service;

import com.dthxhieu.ticket_booking_system_be.auth.dto.request.LoginRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RefreshTokenRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RegisterRequest;
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

}