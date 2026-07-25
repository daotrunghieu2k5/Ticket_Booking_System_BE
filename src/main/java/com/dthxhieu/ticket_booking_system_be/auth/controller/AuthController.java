package com.dthxhieu.ticket_booking_system_be.auth.controller;

import com.dthxhieu.ticket_booking_system_be.auth.dto.request.LoginRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RefreshTokenRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RegisterRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.request.VerifyOtpRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.response.LoginResponse;
import com.dthxhieu.ticket_booking_system_be.auth.dto.response.RefreshTokenResponse;
import com.dthxhieu.ticket_booking_system_be.auth.service.AuthService;
import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Registration successful. Please check your email for the OTP.")
                        .build());
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        authService.verifyOtp(request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Account verified successfully.")
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .success(true)
                .message("Login successfully.")
                .data(response)
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        RefreshTokenResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(ApiResponse.<RefreshTokenResponse>builder()
                .success(true)
                .message("Token refreshed successfully.")
                .data(response)
                .build());
    }
}
