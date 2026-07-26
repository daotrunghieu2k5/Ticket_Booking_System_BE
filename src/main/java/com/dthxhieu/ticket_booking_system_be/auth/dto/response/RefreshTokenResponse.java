package com.dthxhieu.ticket_booking_system_be.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// RefreshTokenResponse carries only the new access token.
// The refresh token itself is NOT rotated in this user story (BR-06),
// so there is no need to return a new refresh token.
// tokenType and expiresIn are included to match the LoginResponse format
// and give the client consistent token management information.
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenResponse {

    private String accessToken;

    private String tokenType;

    private long expiresIn;
}