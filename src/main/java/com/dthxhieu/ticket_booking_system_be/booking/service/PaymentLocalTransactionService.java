package com.dthxhieu.ticket_booking_system_be.booking.service;

import com.dthxhieu.ticket_booking_system_be.entity.booking.PaymentTransaction;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

// PaymentLocalTransactionService — handles DB transactions for the payment creation flow.
//
// This service exists solely to avoid Spring AOP self-invocation problems.
// PaymentServiceImpl is NOT annotated @Transactional, so it calls methods here via the
// Spring proxy (injected bean), ensuring @Transactional(REQUIRES_NEW) is respected.
//
// Architecture:
//   PaymentServiceImpl
//     --> createPendingTransaction()   [T1: create local record, COMMIT]
//     --> PayOS external HTTP call
//     --> updateTransactionWithGateway() [T2: save gateway result, COMMIT]
public interface PaymentLocalTransactionService {

    // Transaction 1: validate, create PaymentTransaction(PENDING), commit.
    // Returns the saved PaymentTransaction with its generated id.
    PaymentTransaction createPendingTransaction(Long paymentId, Long bookingId);

    // Transaction 2: update PaymentTransaction with gateway response, commit.
    // Called after PayOS successfully returns checkoutUrl.
    void updateTransactionWithGateway(Long transactionId, CreatePaymentLinkResponse response);
}
