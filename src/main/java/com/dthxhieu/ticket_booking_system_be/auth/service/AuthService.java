package com.dthxhieu.ticket_booking_system_be.auth.service;

import com.dthxhieu.ticket_booking_system_be.auth.dto.request.RegisterRequest;
import com.dthxhieu.ticket_booking_system_be.auth.dto.response.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

}