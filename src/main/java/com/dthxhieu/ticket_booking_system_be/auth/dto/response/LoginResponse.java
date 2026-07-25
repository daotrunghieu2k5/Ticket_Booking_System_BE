package com.dthxhieu.ticket_booking_system_be.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    // Follows the OAuth2 Bearer Token Response standard.
    // tokenType is always "Bearer" for JWT-based APIs.
    private String tokenType;

    // Expiration in seconds so the frontend can schedule token refresh proactively.
    private long expiresIn;
}

