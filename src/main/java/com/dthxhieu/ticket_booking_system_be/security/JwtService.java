package com.dthxhieu.ticket_booking_system_be.security;

import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import com.dthxhieu.ticket_booking_system_be.entity.auth.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

// JwtService is responsible for JWT generation only.
// Token validation and request interception are handled by JwtAuthenticationFilter (future US).
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    // Constructor injection reads JWT configuration from application.yml.
    // The secret is converted to a SecretKey once at startup - not on every token generation.
    // This avoids repeated key derivation and keeps signing fast.
    public JwtService(
            @Value("${spring.jwt.secret}") String secret,
            @Value("${spring.jwt.access-token-expiration}") long accessTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    // Generates a signed JWT Access Token containing userId, email, and roles.
    // Claims are deliberately minimal - only what the Resource Server needs for authorization.
    // The sub claim holds userId (not email) so the token remains valid if email changes.
    public String generateAccessToken(User user) {
        List<String> roles = user.getUserRoles().stream()
                .map(UserRole::getRole)
                .map(role -> role.getName())
                .toList();

        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // Returns access token expiration in seconds - used in the LoginResponse.expiresIn field.
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}