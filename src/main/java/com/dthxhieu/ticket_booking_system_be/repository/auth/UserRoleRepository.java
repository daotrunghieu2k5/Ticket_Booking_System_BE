package com.dthxhieu.ticket_booking_system_be.repository.auth;

import com.dthxhieu.ticket_booking_system_be.entity.auth.UserRole;
import com.dthxhieu.ticket_booking_system_be.entity.auth.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}