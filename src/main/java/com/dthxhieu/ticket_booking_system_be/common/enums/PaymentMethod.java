package com.dthxhieu.ticket_booking_system_be.common.enums;

// Payment method options per DATABASE.md §12.
//
// US-13 scope: Payment record is created with paymentMethod = null.
// The method is set when the user selects a gateway and initiates payment (US-14).
//
// Only PAYOS is integrated (US-14). VNPAY and MOMO are declared per DATABASE.md
// but have no implementation — future USs will implement them.
public enum PaymentMethod {
    VNPAY,
    PAYOS,
    MOMO
}

