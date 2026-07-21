package com.dthxhieu.ticket_booking_system_be.repository.auth;

import com.dthxhieu.ticket_booking_system_be.entity.auth.RefreshToken;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}