package com.dthxhieu.ticket_booking_system_be.common.exception;

import com.dthxhieu.ticket_booking_system_be.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex
    ) {

        ApiResponse<Void> response =
                ApiResponse.<Void>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex
    ) {

        ApiResponse<Void> response =
                ApiResponse.<Void>builder()
                        .success(false)
                        .message("Internal Server Error")
                        .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

}