# US-14 — Payment Processing with payOS

## 1. Objective

Implement payment processing for the Ticket Booking System using **payOS**.

US14 allows an authenticated user to pay for a Booking created in US13.

The system must:

* Create a payOS payment link.
* Return the checkout URL to the frontend.
* Track the payment using the existing `Payment` entity.
* Record payment gateway transaction information in `PaymentTransaction`.
* Receive and verify payOS webhook notifications.
* Update Payment and Booking status after successful or failed payment.
* Prevent duplicate webhook processing.
* Preserve payment transaction history.

The payment flow must use the existing database relationship:

```text
Booking
   │
   └── Payment
          │
          └── PaymentTransaction
```

The existing database defines:

```text
Booking 1 ---- 1 Payment
Payment 1 ---- * PaymentTransaction
```

and requires payment history and gateway responses to be preserved.

---

# 2. Scope

## 2.1 Included

### Payment

* Create payment for a Booking.
* Validate Booking ownership.
* Validate Booking status.
* Validate Payment status.
* Validate payment amount.
* Create payOS payment link.
* Return payOS checkout URL.
* Store payment method as `PAYOS`.

### PaymentTransaction

* Create transaction record when payment processing starts.
* Store payOS transaction information.
* Store gateway response.
* Store transaction status.
* Preserve transaction history.

### Webhook

* Receive payOS webhook.
* Verify webhook signature.
* Validate `orderCode`.
* Validate payment amount.
* Find corresponding Payment.
* Update PaymentTransaction.
* Update Payment.
* Update Booking.
* Handle duplicate webhook requests safely.

### Payment Result

Successful payment:

```text
Payment → SUCCESS
Booking → PAID
PaymentTransaction → SUCCESS
```

Failed payment:

```text
Payment → FAILED
PaymentTransaction → FAILED
Booking remains WAITING_PAYMENT
```

---

## 2.2 Not Included

Do not implement the following in US14:

* Refund
* Partial refund
* Payment reversal
* Multiple payment gateways
* VNPay
* MOMO
* Wallet
* Bank account management
* Payout
* Invoice
* Notification
* Email after payment
* QR ticket generation
* Ticket scanning
* Admin payment management
* Payment reconciliation system
* Microservices
* Kafka
* Redis

Only **payOS payment processing** is implemented.

---

# 3. Business Flow

## 3.1 Create Payment

```text
Authenticated User
        ↓
POST /api/v1/payments/{bookingId}
        ↓
Authenticate current user
        ↓
Find Booking
        ↓
Booking exists?
   ├── NO → BusinessException
   └── YES
        ↓
Check Booking ownership
        ↓
Check Booking status
        ↓
Find Payment
        ↓
Payment already SUCCESS?
   ├── YES → BusinessException
   └── NO
        ↓
Validate Payment amount
        ↓
Create unique payOS orderCode
        ↓
Create PaymentTransaction(PENDING)
        ↓
Call payOS API
        ↓
Payment link created?
   ├── NO → transaction FAILED
   └── YES
        ↓
Return checkoutUrl
```

payOS provides a Java SDK for creating payment links through `paymentRequests().create()`.

---

## 3.2 User Payment

```text
Frontend
   ↓
checkoutUrl
   ↓
payOS Checkout
   ↓
User scans VietQR
   ↓
Bank payment
   ↓
payOS processes transaction
```

payOS's documented flow is to redirect the customer to the payment checkout, where the customer can use a banking application to scan the VietQR generated for the payment.

---

## 3.3 Return URL

After payment, payOS redirects the browser to the configured `returnUrl`.

```text
payOS
   ↓
Frontend returnUrl
   ↓
Display payment result
```

The return URL can contain:

```text
code
id
cancel
status
orderCode
```

according to the payOS return URL documentation.

### Important

`returnUrl` must **not** be trusted as the final source of payment confirmation.

The backend must use the verified webhook to update payment state.

---

## 3.4 Webhook

```text
payOS
   ↓
POST /api/v1/payments/webhook/payos
   ↓
Receive webhook
   ↓
Verify signature
        ↓
Signature valid?
   ├── NO → Reject webhook
   └── YES
        ↓
Read orderCode
        ↓
Find PaymentTransaction
        ↓
Transaction already processed?
   ├── YES → Return success / ignore duplicate
   └── NO
        ↓
Validate amount
        ↓
Create / update PaymentTransaction
        ↓
Payment successful?
   ├── NO → Payment FAILED
   │         Booking remains WAITING_PAYMENT
   │
   └── YES
        ↓
PaymentTransaction SUCCESS
        ↓
Payment SUCCESS
        ↓
Booking PAID
        ↓
Commit
```

payOS webhook data includes information such as `orderCode`, `amount`, `reference`, `paymentLinkId`, transaction time and a `signature`; the signature is used to verify the authenticity/integrity of the webhook data.

---

# 4. Functional Requirements

## FR-01 — Create Payment

Authenticated users can create a payment for their own eligible Booking.

---

## FR-02 — Booking Ownership

The system must verify that the current authenticated user owns the Booking.

The client must not be trusted to provide the user identity.

---

## FR-03 — Booking Status

Payment can only be initiated for a Booking that is waiting for payment.

Recommended state:

```text
WAITING_PAYMENT
```

The system must reject payment creation when Booking is already:

```text
PAID
COMPLETED
CANCELLED
EXPIRED
```

---

## FR-04 — Payment Status

A Payment can start with:

```text
PENDING
```

and may transition to:

```text
SUCCESS
FAILED
```

`REFUNDED` is reserved for a future refund feature and must not be created by US14.

The existing database defines:

```text
PaymentStatus

PENDING
SUCCESS
FAILED
REFUNDED
```

---

## FR-05 — Create payOS Payment Link

The backend must create a payment link using payOS.

The request must contain at minimum:

```text
orderCode
amount
description
cancelUrl
returnUrl
```

payOS currently documents these fields for payment-link creation and returns a `checkoutUrl` that the merchant can use to send the customer to checkout.

---

## FR-06 — Return Checkout URL

The backend must return the payOS checkout URL to the frontend.

Example:

```json
{
  "paymentId": 100,
  "bookingId": 500,
  "paymentMethod": "PAYOS",
  "status": "PENDING",
  "checkoutUrl": "https://pay.payos.vn/..."
}
```

The frontend is responsible only for redirecting the user to the payment page.

---

## FR-07 — PaymentTransaction

Every payment attempt must be represented by a `PaymentTransaction`.

The existing database allows:

```text
Payment
   ↓
1:N
   ↓
PaymentTransaction
```

and explicitly supports multiple transaction attempts.

---

## FR-08 — Transaction Status

The initial transaction status is:

```text
PENDING
```

Possible final states:

```text
SUCCESS
FAILED
```

These match the existing `PaymentTransactionStatus` enum.

---

## FR-09 — Webhook Verification

The backend must verify the payOS webhook signature before processing payment data.

Invalid signatures must be rejected.

payOS documents HMAC-SHA256-based signature verification for payment webhook data and also provides verification support through its Java SDK.

---

## FR-10 — Amount Verification

The backend must verify:

```text
webhook.amount
==
Payment.amount
==
Booking.total_amount
```

The system must never mark a Payment as successful when the received amount does not match the expected amount.

---

## FR-11 — Payment Success

When a valid successful webhook is received:

```text
PaymentTransaction.status
        ↓
SUCCESS

Payment.status
        ↓
SUCCESS

Booking.status
        ↓
PAID
```

All changes must happen in one transaction.

---

## FR-12 — Payment Failure

When the payment fails:

```text
PaymentTransaction.status
        ↓
FAILED

Payment.status
        ↓
FAILED
```

Booking must not become `PAID`.

The Booking may remain:

```text
WAITING_PAYMENT
```

according to the existing booking lifecycle.

---

## FR-13 — Idempotent Webhook

The same webhook must not be processed multiple times.

If the same transaction is received again:

```text
First webhook
    ↓
Process

Second webhook
    ↓
Detect existing transaction
    ↓
Do not duplicate transaction
    ↓
Return successful acknowledgement
```

---

## FR-14 — Payment History

Payment and PaymentTransaction records must never be physically deleted.

The database explicitly requires payment history to be preserved.

---

## FR-15 — Gateway Payload

The original payOS gateway response must be stored in:

```text
payment_transaction.gateway_payload
```

This field exists specifically for auditing gateway responses.

---

## FR-16 — API Response

All application APIs must use:

```text
ApiResponse<T>
```

according to the project's API conventions.

---

# 5. Business Rules

## BR-01 — Only authenticated users

Only authenticated users can initiate payment.

---

## BR-02 — Booking ownership

A user can only pay for their own Booking.

Never trust:

```text
userId
```

from the request.

Use the authenticated user from Spring Security.

---

## BR-03 — Booking must be payable

Only:

```text
WAITING_PAYMENT
```

Booking can initiate payment.

---

## BR-04 — One Payment per Booking

The database defines:

```text
UNIQUE(payment.booking_id)
```

Therefore one Booking can only have one Payment.

US14 must reuse the existing Payment created by US13 rather than creating another Payment.

---

## BR-05 — Payment amount

The backend must use:

```text
Payment.amount
```

and verify that:

```text
Payment.amount
==
Booking.total_amount
```

The database explicitly defines this constraint.

---

## BR-06 — payOS amount

The amount sent to payOS must equal:

```text
Booking.total_amount
```

The frontend must never provide the authoritative payment amount.

---

## BR-07 — orderCode

Every payOS payment request must have a unique `orderCode`.

The generated `orderCode` must allow the backend to identify the corresponding PaymentTransaction.

Do not use a random value that cannot be mapped back to the local transaction.

---

## BR-08 — Transaction code

The database requires:

```text
UNIQUE(payment_transaction.transaction_code)
```

Therefore the implementation must not insert duplicate transaction codes.

---

## BR-09 — Gateway provider

For US14:

```text
provider = PAYOS
```

and:

```text
payment_method = PAYOS
```

The existing `PaymentMethod` enum contains:

```text
VNPAY
PAYOS
MOMO
```

but US14 only implements `PAYOS`.

---

## BR-10 — Webhook is authoritative

The frontend return URL must not directly update:

```text
Payment.SUCCESS
Booking.PAID
```

The backend must wait for a verified payment result from payOS webhook processing.

---

## BR-11 — Signature verification

Never process payment status from an unverified webhook.

The order must be:

```text
Receive webhook
      ↓
Verify signature
      ↓
Validate payload
      ↓
Process transaction
```

---

## BR-12 — Amount verification

Before changing Payment to `SUCCESS`:

```text
webhook.amount
==
payment.amount
```

If they differ:

```text
Reject
```

and do not mark the payment successful.

---

## BR-13 — Idempotency

Webhook processing must be idempotent.

A repeated webhook for the same transaction must not:

* create another transaction
* change the payment incorrectly
* create another Booking
* create another Payment

---

## BR-14 — Payment history

Never physically delete:

```text
Payment
PaymentTransaction
```

Payment history is part of the system audit trail.

---

## BR-15 — No direct client success

The client must never be able to send:

```json
{
  "status": "SUCCESS"
}
```

and cause:

```text
Payment → SUCCESS
Booking → PAID
```

Only trusted backend payment processing may perform this transition.

---

# 6. API Contract

## 6.1 Create Payment

### Endpoint

```http
POST /api/v1/payments/{bookingId}
```

### Authentication

Required.

### Request Body

No payment amount is accepted from the client.

```json
{}
```

The backend obtains the amount from the existing Booking/Payment.

---

### Success Response

```json
{
  "success": true,
  "message": "Payment link created successfully.",
  "data": {
    "paymentId": 100,
    "bookingId": 500,
    "paymentMethod": "PAYOS",
    "status": "PENDING",
    "amount": 450000,
    "checkoutUrl": "https://pay.payos.vn/...",
    "expiresAt": "2026-08-12T21:30:00"
  },
  "timestamp": "2026-08-12T21:20:00"
}
```

The exact `expiresAt` field should only be returned if the implementation stores or can reliably derive the payment-link expiration time.

---

## 6.2 Get Payment Detail

### Endpoint

```http
GET /api/v1/payments/{paymentId}
```

### Authentication

Required.

### Rules

The user must own the Booking associated with the Payment.

### Response

```json
{
  "success": true,
  "message": "Payment retrieved successfully.",
  "data": {
    "id": 100,
    "bookingId": 500,
    "amount": 450000,
    "paymentMethod": "PAYOS",
    "status": "PENDING",
    "createdAt": "2026-08-12T21:20:00",
    "updatedAt": "2026-08-12T21:20:00"
  },
  "timestamp": "2026-08-12T21:21:00"
}
```

---

## 6.3 Get Payment by Booking

### Endpoint

```http
GET /api/v1/bookings/{bookingId}/payment
```

### Authentication

Required.

### Rules

The user must own the Booking.

### Response

```json
{
  "success": true,
  "message": "Payment retrieved successfully.",
  "data": {
    "id": 100,
    "bookingId": 500,
    "amount": 450000,
    "paymentMethod": "PAYOS",
    "status": "SUCCESS"
  },
  "timestamp": "2026-08-12T21:30:00"
}
```

---

## 6.4 payOS Webhook

### Endpoint

```http
POST /api/v1/payments/webhook/payos
```

### Authentication

This endpoint must be publicly accessible to payOS.

Do not require JWT authentication.

The webhook is authenticated using payOS signature verification.

### Request

The backend receives the payOS webhook payload.

Conceptually:

```json
{
  "code": "00",
  "desc": "success",
  "success": true,
  "data": {
    "orderCode": 123456,
    "amount": 450000,
    "description": "BOOKING-500",
    "accountNumber": "...",
    "reference": "...",
    "transactionDateTime": "2026-08-12 21:30:00",
    "currency": "VND",
    "paymentLinkId": "...",
    "code": "00",
    "desc": "Thành công"
  },
  "signature": "..."
}
```

The exact payload is determined by payOS and must be mapped to the project's DTO rather than exposed directly to application/domain entities.

### Successful acknowledgement

Return an HTTP `2xx` response after the webhook has been successfully verified and processed.

payOS documents that a 2xx response is used to acknowledge successful webhook receipt.

---

## 6.5 Payment Result

The frontend may receive a return URL such as:

```text
/api/payment/result
```

with payOS query parameters.

The frontend should use this information only to display the payment result or navigate to the Booking page.

The authoritative payment state must be obtained from the backend after webhook processing.

---

# 7. Validation Rules

## Create Payment

### bookingId

* required
* must be a valid ID
* Booking must exist
* Booking must belong to current user
* Booking status must allow payment

---

## Payment

Before creating the payOS payment link:

```text
Payment exists
Payment.status == PENDING
Payment.amount == Booking.total_amount
Payment.payment_method == PAYOS
```

If US13 has already created the Payment with another method, update the method only according to the project's established business flow. Do not silently overwrite an already successful payment.

---

## Webhook

The following must be validated:

### Signature

Must be valid.

### orderCode

Must identify a local PaymentTransaction.

### Amount

Must equal:

```text
Payment.amount
```

### Currency

Expected:

```text
VND
```

unless the project later supports multiple currencies.

### Transaction status

Only valid payOS success/failure states may update the local transaction.

---

## Do not validate payment success from frontend

Never use:

```text
returnUrl status = PAID
```

as the sole condition for:

```text
Payment.SUCCESS
```

The backend must process the verified webhook.

---

# 8. Database Impact

## Read

```text
booking
payment
payment_transaction
users
```

---

## Insert

```text
payment_transaction
```

if a new transaction attempt is created.

US14 should normally **reuse the Payment created by US13**.

---

## Update

### payment

Possible updates:

```text
status
payment_method
updated_at
```

### payment_transaction

Possible updates:

```text
status
transaction_code
response_code
response_message
gateway_payload
transaction_time
```

### booking

Possible update:

```text
status
updated_at
```

---

## Delete

None.

Payment history must never be physically deleted.

The database explicitly requires payment history to be preserved.

---

## Schema Changes

US14 should **not require a new table**.

The existing database already contains:

```text
payment
payment_transaction
```

with the required fields for gateway integration.

Do not create:

```text
payos_payment
payos_transaction
payment_gateway
```

tables.

---

# 9. Expected Classes

## Controller

```text
PaymentController
```

Responsibilities:

* create payment
* get payment
* get payment by booking

Keep the controller thin.

---

## Webhook Controller

Recommended:

```text
PaymentWebhookController
```

Responsibilities:

* receive payOS webhook
* delegate processing to service
* return acknowledgement

Do not put payment business logic inside the controller.

---

## Service

```text
PaymentService
PaymentServiceImpl
```

Responsibilities:

* validate Booking
* validate ownership
* validate Payment
* create payOS payment link
* create PaymentTransaction
* return payment response

---

## Webhook Service

```text
PaymentWebhookService
PaymentWebhookServiceImpl
```

Responsibilities:

* verify webhook
* locate transaction
* validate amount
* process success/failure
* update Payment
* update Booking
* ensure idempotency

---

## Repository

Existing repositories:

```text
BookingRepository
PaymentRepository
PaymentTransactionRepository
```

Add only the query methods actually required.

Examples:

```java
Optional<Payment> findByBookingId(Long bookingId);

Optional<PaymentTransaction>
findByTransactionCode(String transactionCode);
```

If `orderCode` is not persisted in an existing field, the implementation must first resolve how the payOS order code maps to the local transaction without modifying the database unnecessarily.

---

## DTOs

### Request

```text
CreatePaymentRequest
```

This may be unnecessary if the endpoint only receives `bookingId` in the path.

Prefer no request body if no client input is required.

### Response

```text
PaymentResponse
PaymentLinkResponse
```

### Webhook

```text
PayOSWebhookRequest
PayOSWebhookData
```

These DTOs should represent external payOS payloads and should not be reused as domain entities.

---

## Integration

```text
PayOSService
PayOSServiceImpl
```

Responsibilities:

* create payment link
* verify webhook
* communicate with payOS SDK

The payOS Java SDK currently provides `PayOS.fromEnv()`, payment-link creation and webhook verification APIs.

---

## Configuration

Recommended configuration:

```text
PayOSProperties
```

with values loaded from environment variables:

```text
PAYOS_CLIENT_ID
PAYOS_API_KEY
PAYOS_CHECKSUM_KEY
PAYOS_RETURN_URL
PAYOS_CANCEL_URL
```

Do not hard-code these credentials.

payOS documents initialization through environment variables including `PAYOS_CLIENT_ID`, `PAYOS_API_KEY` and `PAYOS_CHECKSUM_KEY`.

---

# 10. Transaction

## Create Payment

The service should use a transaction when creating/updating the local PaymentTransaction state.

However, the external payOS API call must be treated carefully.

Recommended flow:

```text
BEGIN TRANSACTION
      ↓
Load Booking
      ↓
Validate ownership
      ↓
Load Payment
      ↓
Validate Payment
      ↓
Create local transaction state
      ↓
COMMIT local state
      ↓
Call payOS
      ↓
Update local transaction with gateway result
```

The implementation must avoid keeping a database transaction open while waiting unnecessarily for an external network request.

---

## Webhook Transaction

Webhook processing should be transactional:

```java
@Transactional
```

Flow:

```text
Receive webhook
      ↓
Verify signature
      ↓
Find local transaction
      ↓
Check idempotency
      ↓
Validate amount
      ↓
Update PaymentTransaction
      ↓
Update Payment
      ↓
Update Booking
      ↓
COMMIT
```

If any database operation fails:

```text
ROLLBACK
```

No partial payment state should be committed.

---

# 11. Exception Cases

Use existing `BusinessException` and project-specific exception handling where possible.

Do not create unnecessary exception classes.

Possible business errors:

### BookingNotFoundException

Booking does not exist.

### BookingAccessDeniedException

Current user does not own the Booking.

### BookingNotPayableException

Booking is not in `WAITING_PAYMENT`.

### PaymentNotFoundException

Payment does not exist for the Booking.

### PaymentAlreadyCompletedException

Payment is already `SUCCESS`.

### PaymentAmountMismatchException

Payment amount differs from Booking total.

### PaymentTransactionNotFoundException

Webhook references an unknown local transaction.

### InvalidPayOSWebhookException

Webhook signature or payload is invalid.

### PaymentAlreadyProcessedException

Transaction has already been processed.

### PayOSIntegrationException

Communication with payOS fails.

Map these through the existing `GlobalExceptionHandler`.

Do not expose:

```text
API key
Checksum key
raw stack trace
internal gateway exception
```

to the client.

---

# 12. Acceptance Criteria

## Payment Creation

* [ ] Authenticated user can create payment for their own Booking.
* [ ] Unauthenticated user cannot create payment.
* [ ] User cannot pay for another user's Booking.
* [ ] Booking must be `WAITING_PAYMENT`.
* [ ] Existing Payment is reused.
* [ ] Payment amount equals Booking total.
* [ ] Payment method is `PAYOS`.
* [ ] payOS payment link is successfully created.
* [ ] Backend returns checkout URL.

## Payment Gateway

* [ ] payOS credentials are loaded from environment/configuration.
* [ ] Credentials are never hard-coded.
* [ ] Backend does not expose credentials.
* [ ] payOS integration is isolated from domain logic.

## Transaction

* [ ] PaymentTransaction is created/updated correctly.
* [ ] Provider is `PAYOS`.
* [ ] Transaction status starts as `PENDING`.
* [ ] Gateway response is stored.
* [ ] Transaction code is unique.

## Webhook

* [ ] Webhook endpoint is publicly reachable by payOS.
* [ ] Webhook signature is verified.
* [ ] Invalid signature is rejected.
* [ ] orderCode is mapped to local payment transaction.
* [ ] Amount is validated.
* [ ] Duplicate webhook does not create duplicate transaction.
* [ ] Successful webhook updates Payment to `SUCCESS`.
* [ ] Successful webhook updates Booking to `PAID`.
* [ ] Failed webhook does not mark Booking as `PAID`.

## History

* [ ] Payment is never physically deleted.
* [ ] PaymentTransaction is never physically deleted.
* [ ] Gateway payload is preserved.

## API

* [ ] APIs use `ApiResponse<T>`.
* [ ] Error handling uses `GlobalExceptionHandler`.
* [ ] Sensitive gateway information is never returned.
* [ ] Controller remains thin.

---

# 13. Coding Constraints

Follow:

```text
PROJECT_CONTEXT.md
DATABASE.md
US13.md
```

Rules:

* Use Java 21.
* Use Spring Boot 4.1.
* Use Spring Data JPA.
* Use Spring Security.
* Use PostgreSQL.
* Use Flyway.
* Use Lombok where the existing project already uses it.
* Use constructor injection.
* Use `@Transactional` for database write flows.
* Use `ApiResponse<T>`.
* Use `BusinessException`.
* Use Jakarta Validation where applicable.
* Use `BigDecimal` for monetary values.
* Use `LocalDateTime` for timestamps.
* Use `@Enumerated(EnumType.STRING)` for enums.
* Use `FetchType.LAZY`.
* Do not expose entities through REST.
* Do not modify completed Flyway migrations.
* Do not create unnecessary tables.
* Do not create a separate PayOS entity.
* Do not create a separate PayOS database.
* Do not implement other payment providers.
* Do not implement refunds.
* Do not implement notifications.
* Do not introduce Redis/Kafka.
* Do not change unrelated modules.

The project database explicitly requires `BigDecimal`, string-based enums, LAZY relationships and avoiding unnecessary abstractions.

---

# 14. Review Checklist

Before considering US14 complete:

## Architecture

* [ ] Controller is thin.
* [ ] Service contains business logic.
* [ ] PayOS integration is isolated.
* [ ] External DTOs are separated from internal domain objects.
* [ ] Existing database structure is reused.

## Security

* [ ] PayOS credentials are stored securely.
* [ ] Secrets are not logged.
* [ ] Webhook signature is verified.
* [ ] Booking ownership is checked.
* [ ] Payment status cannot be manipulated by frontend.

## Payment

* [ ] Payment amount comes from backend.
* [ ] Payment amount equals Booking total.
* [ ] Payment method is PAYOS.
* [ ] Payment status transitions are valid.
* [ ] Transaction history is preserved.

## Webhook

* [ ] Signature verification works.
* [ ] orderCode mapping works.
* [ ] Amount verification works.
* [ ] Duplicate webhook is handled.
* [ ] Successful webhook updates all required entities atomically.
* [ ] Invalid webhook cannot change payment status.

## Database

* [ ] No unnecessary migration.
* [ ] Existing UNIQUE constraints are respected.
* [ ] `payment.booking_id` remains unique.
* [ ] `payment_transaction.transaction_code` remains unique.
* [ ] No payment history is deleted.

## Testing

* [ ] Create payment success.
* [ ] Booking not found.
* [ ] Booking belongs to another user.
* [ ] Booking already paid.
* [ ] payOS API failure.
* [ ] Invalid webhook signature.
* [ ] Unknown orderCode.
* [ ] Amount mismatch.
* [ ] Successful webhook.
* [ ] Failed webhook.
* [ ] Duplicate webhook.
* [ ] Concurrent webhook processing.

---

# 15. Architecture Decisions

## Decision 1 — Use payOS as the payment provider

### Decision

US14 integrates payOS as the first payment gateway.

### Why

The project needs a realistic online payment flow while keeping the implementation suitable for a Java Backend Internship project.

payOS provides:

* Java SDK
* payment-link API
* VietQR checkout
* webhook
* webhook signature verification

### Alternative

Implement a manual bank-transfer QR.

### Trade-off

payOS requires external configuration and credentials, but provides a much more realistic payment lifecycle.

---

## Decision 2 — Use existing Payment and PaymentTransaction tables

### Decision

Do not create:

```text
payos_payment
payos_transaction
```

### Why

The current database already models payment-provider transactions generically:

```text
Payment
   ↓
PaymentTransaction
```

with:

```text
provider
transaction_code
request_id
response_code
response_message
gateway_payload
status
transaction_time
```

### Alternative

Create provider-specific tables.

### Trade-off

A generic model is simpler and allows future providers such as VNPay or MOMO without changing the core payment model.

---

## Decision 3 — Webhook is the authoritative payment confirmation

### Decision

Use verified payOS webhook data to update Payment and Booking.

### Why

The browser return URL is controlled by the client-side navigation flow. The webhook is the server-to-server payment notification mechanism documented by payOS.

### Alternative

Trust the `returnUrl` status.

### Trade-off

Trusting return URL is simpler but unsafe.

---

## Decision 4 — Verify webhook signature

### Decision

Every webhook must be verified before processing.

### Why

It prevents unauthorized requests from pretending to be payOS.

payOS documents HMAC-SHA256 signature verification and the Java SDK provides webhook verification.

### Alternative

Trust the webhook endpoint because the URL is secret.

### Trade-off

URL secrecy is not authentication. Signature verification provides actual integrity/authenticity validation.

---

## Decision 5 — Use idempotent webhook processing

### Decision

Repeated webhook notifications must not create duplicate transactions.

### Why

External systems may retry webhook delivery.

### Alternative

Always insert a new transaction.

### Trade-off

A new record for every notification could duplicate the same gateway transaction and corrupt payment history.

---

# 16. Knowledge Check

After implementation, explain the code as if mentoring a Java Backend Intern.

## Spring

Explain:

1. `@Transactional`
2. `@RestController`
3. `@Service`
4. `@Repository`
5. Constructor injection
6. `@ConfigurationProperties`
7. Spring Security authentication

---

## Payment Architecture

The developer should understand:

```text
Booking
   ↓
Payment
   ↓
PaymentTransaction
   ↓
payOS
```

and why `Payment` and `PaymentTransaction` are different concepts.

---

## Payment vs Transaction

Explain:

```text
Payment
```

represents the payment belonging to a Booking.

```text
PaymentTransaction
```

represents an individual attempt/transaction with the payment gateway.

Therefore:

```text
Payment 1 : N PaymentTransaction
```

is required by the database design.

---

## Webhook

The developer should understand:

```text
payOS
 ↓
Webhook
 ↓
Signature Verification
 ↓
Find Transaction
 ↓
Validate Amount
 ↓
Update Transaction
 ↓
Update Payment
 ↓
Update Booking
```

---

## Security Questions

Possible interview questions:

1. Why should we not trust the payment status from frontend?
2. Why must webhook signatures be verified?
3. Why should payment credentials not be hard-coded?
4. Why should Booking ownership be checked?
5. Why should payment history never be deleted?

---

## Concurrency Questions

Possible interview questions:

1. What happens if payOS sends the same webhook twice?
2. How do you implement idempotency?
3. Why do we need a unique transaction code?
4. Why is a database constraint useful even when Service validation exists?

---

## Architecture Questions

Possible interview questions:

1. Why separate `PaymentService` from `PayOSService`?
2. Why should the Controller not call the payOS SDK directly?
3. Why use `PaymentTransaction` instead of storing only Payment status?
4. Why is webhook processing transactional?
5. Why should the return URL not determine the final payment status?

---

# 17. Implementation Boundary

US14 starts from the Payment created by US13:

```text
US13
   ↓
Booking
   ↓
Payment(PENDING)
```

US14 implements:

```text
Payment(PENDING)
   ↓
Create payOS payment link
   ↓
User pays
   ↓
payOS webhook
   ↓
PaymentTransaction
   ↓
Payment SUCCESS / FAILED
   ↓
Booking PAID
```

US14 ends after the backend has correctly processed the payment result.

Do not implement:

```text
Refund
QR Ticket
Ticket Scanning
Notification
Invoice
Admin Payment Dashboard
```

Those belong to later features.

# End of US14
