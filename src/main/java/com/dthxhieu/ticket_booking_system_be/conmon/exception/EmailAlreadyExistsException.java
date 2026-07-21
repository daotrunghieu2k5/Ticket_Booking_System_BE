package com.dthxhieu.ticket_booking_system_be.conmon.exception;

public class EmailAlreadyExistsException extends BusinessException {

    public EmailAlreadyExistsException() {
        super("Email already exists.");
    }

}