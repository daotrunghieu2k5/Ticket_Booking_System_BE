package com.dthxhieu.ticket_booking_system_be.common.enums;

// Payment method per DATABASE.md §12.
// US-13 does not use a payment method (gateway integration is future US).
// payment_method column is nullable when a Payment is first created (PENDING).
public enum PaymentMethod {
    VNPAY,
    PAYOS,
    MOMO
}
