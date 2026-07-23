package com.dthxhieu.ticket_booking_system_be.auth.dto.response;

import com.dthxhieu.ticket_booking_system_be.conmon.enums.UserStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {

    private Long id;

    private String fullName;

    private String email;

    private UserStatus status;

    private boolean emailVerified;
}