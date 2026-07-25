package com.dthxhieu.ticket_booking_system_be.auth.mapper;

import com.dthxhieu.ticket_booking_system_be.auth.dto.response.RegisterResponse;
import com.dthxhieu.ticket_booking_system_be.entity.auth.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public RegisterResponse toRegisterResponse(User user) {

        return RegisterResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .build();
    }
}