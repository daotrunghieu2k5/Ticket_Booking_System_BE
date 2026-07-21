package com.dthxhieu.ticket_booking_system_be.repository.auth;

import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}