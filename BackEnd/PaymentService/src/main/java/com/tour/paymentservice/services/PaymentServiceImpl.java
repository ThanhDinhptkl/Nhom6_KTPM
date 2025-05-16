package com.tour.paymentservice.services;

import org.springframework.stereotype.Service;

import com.tour.paymentservice.dto.PaymentRequestDto;
import com.tour.paymentservice.dto.PaymentResponseDto;
import com.tour.paymentservice.entities.Payment;
import com.tour.paymentservice.entities.PaymentMethod;
import com.tour.paymentservice.entities.PaymentStatus;
import com.tour.paymentservice.repositories.PaymentRepository;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final MomoPaymentService momoPaymentService;
    private final VnPayService vnPayService;
    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;

    // Payment validity period in minutes (15 minutes)
    private static final long PAYMENT_VALIDITY_MINUTES = 15;

    /**
     * Rate limited createPayment method - maximum 5 payment attempts per 60 seconds
     */
    @Override
    @RateLimiter(name = "createPayment", fallbackMethod = "createPaymentFallback")
    @TimeLimiter(name = "createPayment")
    public CompletableFuture<PaymentResponseDto> createPayment(PaymentRequestDto request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Creating payment request: orderId={}, method={}", request.getOrderId(),
                    request.getPaymentMethod());

            // Get list of existing payments for this order ID
            List<Payment> existingPayments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(request.getOrderId());

            if (!existingPayments.isEmpty()) {
                // Get most recent payment
                Payment latestPayment = existingPayments.get(0);

                log.info("Found existing payment: id={}, status={}, method={}, created={}",
                        latestPayment.getId(), latestPayment.getStatus(),
                        latestPayment.getPaymentMethod(), latestPayment.getCreatedAt());

                // If payment method is different from requested method, create a new payment
                if (latestPayment.getPaymentMethod() != request.getPaymentMethod()) {
                    log.info(
                            "Requested payment method {} differs from existing payment method {}, creating new payment",
                            request.getPaymentMethod(), latestPayment.getPaymentMethod());
                    return createNewPaymentSync(request);
                }

                // If payment is already completed or failed, create a new one
                if (latestPayment.getStatus() == PaymentStatus.COMPLETED ||
                        latestPayment.getStatus() == PaymentStatus.FAILED) {
                    log.info("Existing payment is {}, creating new payment", latestPayment.getStatus());
                    return createNewPaymentSync(request);
                }

                // If payment is pending but not expired, return the existing payment
                boolean isExpired = isPaymentExpired(latestPayment);

                if (!isExpired && latestPayment.getStatus() == PaymentStatus.PENDING) {
                    log.info("Returning existing non-expired PENDING payment");
                    return modelPaymentToResponse(latestPayment);
                }

                // If payment is expired, create a new one
                if (isExpired) {
                    log.info("Existing payment is expired, creating new payment");
                    return createNewPaymentSync(request);
                }
            }

            // No existing payment, create a new one
            log.info("No existing payment found, creating new payment");
            return createNewPaymentSync(request);
        });
    }

    /**
     * Fallback method for rate limiter
     */
    public CompletableFuture<PaymentResponseDto> createPaymentFallback(PaymentRequestDto request, Throwable t) {
        return CompletableFuture.supplyAsync(() -> {
            log.warn("Rate limit exceeded for creating payment: {}", t.getMessage());

            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .transactionId("RATE_LIMITED")
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .status(PaymentStatus.FAILED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .responseCode("429")
                    .responseMessage("Too many payment attempts. Please try again later.")
                    .customerEmail(request.getCustomerEmail())
                    .description(request.getDescription())
                    .build();

            PaymentResponseDto response = modelPaymentToResponse(payment);
            response.setPaymentUrl(null);
            return response;
        });
    }

    private PaymentResponseDto createNewPaymentSync(PaymentRequestDto request) {
        try {
            // Choose the payment gateway based on payment method
            if (request.getPaymentMethod() == PaymentMethod.MOMO) {
                return momoPaymentService.createMomoPayment(request).join();
            } else if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
                return vnPayService.createVnPayPayment(request).join();
            }

            throw new IllegalArgumentException("Unsupported payment method: " + request.getPaymentMethod());
        } catch (Exception e) {
            log.error("Error in payment gateway: {}", e.getMessage(), e);

            // Create a failed payment response
            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .transactionId(UUID.randomUUID().toString())
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .status(PaymentStatus.FAILED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .responseCode("ERROR")
                    .responseMessage("Payment gateway error: " + e.getMessage())
                    .customerEmail(request.getCustomerEmail())
                    .description(request.getDescription())
                    .build();

            payment = paymentRepository.save(payment);
            return modelPaymentToResponse(payment);
        }
    }

    /**
     * Creates a new payment asynchronously without joining
     */
    private CompletableFuture<PaymentResponseDto> createNewPayment(PaymentRequestDto request) {
        try {
            // Choose the payment gateway based on payment method
            if (request.getPaymentMethod() == PaymentMethod.MOMO) {
                return momoPaymentService.createMomoPayment(request);
            } else if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
                return vnPayService.createVnPayPayment(request);
            }

            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unsupported payment method: " + request.getPaymentMethod()));
        } catch (Exception e) {
            log.error("Error in payment gateway: {}", e.getMessage(), e);

            // Create a failed payment response
            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .transactionId(UUID.randomUUID().toString())
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .status(PaymentStatus.FAILED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .responseCode("ERROR")
                    .responseMessage("Payment gateway error: " + e.getMessage())
                    .customerEmail(request.getCustomerEmail())
                    .description(request.getDescription())
                    .build();

            payment = paymentRepository.save(payment);
            return CompletableFuture.completedFuture(modelPaymentToResponse(payment));
        }
    }

    private boolean isPaymentExpired(Payment payment) {
        if (payment.getCreatedAt() == null) {
            return true;
        }

        LocalDateTime expiryTime = payment.getCreatedAt().plus(PAYMENT_VALIDITY_MINUTES, ChronoUnit.MINUTES);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    private PaymentResponseDto modelPaymentToResponse(Payment payment) {
        return PaymentResponseDto.builder()
                .orderId(payment.getOrderId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paymentUrl(payment.getPaymentUrl())
                .responseCode(payment.getResponseCode())
                .responseMessage(payment.getResponseMessage())
                .build();
    }

    @Override
    public PaymentResponseDto getPaymentByOrderId(String orderId) {
        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (payments.isEmpty()) {
            return null;
        }

        // Return the most recent payment
        return modelPaymentToResponse(payments.get(0));
    }

    /**
     * Update payment status and notify booking service if needed
     * 
     * @param orderId         Order ID
     * @param status          New payment status
     * @param transactionId   Payment gateway transaction ID
     * @param responseCode    Response code from payment gateway
     * @param responseMessage Response message
     * @param paymentMethod   The payment method that completed the payment
     * @return Updated payment response
     */
    public PaymentResponseDto updatePaymentStatus(String orderId, PaymentStatus status,
            String transactionId, String responseCode, String responseMessage, PaymentMethod paymentMethod) {

        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (payments.isEmpty()) {
            log.error("Payment not found for order ID: {}", orderId);
            return null;
        }

        // Find the payment with matching payment method
        Payment payment = null;
        if (paymentMethod != null) {
            // Find the most recent payment matching the specified payment method
            for (Payment p : payments) {
                if (p.getPaymentMethod() == paymentMethod) {
                    payment = p;
                    break;
                }
            }

            if (payment == null) {
                log.warn("No payment found with method {} for orderID: {}, using most recent payment", paymentMethod,
                        orderId);
                payment = payments.get(0);
            } else {
                log.info("Found matching payment with method {} for orderID: {}", paymentMethod, orderId);
            }
        } else {
            // If no payment method is specified, use the most recent one
            payment = payments.get(0);
            log.warn("No payment method specified, using most recent payment for orderID: {}", orderId);
        }

        // Update payment status
        payment.setStatus(status);
        if (transactionId != null) {
            payment.setTransactionId(transactionId);
        }
        payment.setResponseCode(responseCode);
        payment.setResponseMessage(responseMessage);
        payment.setUpdatedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);

        // If status is COMPLETED or FAILED, notify booking service
        if (status == PaymentStatus.COMPLETED || status == PaymentStatus.FAILED) {
            try {
                // Use the payment method from the matched payment for the notification
                PaymentMethod methodToNotify = payment.getPaymentMethod();
                boolean notified = bookingServiceClient.notifyPaymentCompletion(orderId, status.name(), methodToNotify);
                if (notified) {
                    log.info("Successfully notified booking service about {} payment {}", methodToNotify,
                            status.name());
                } else {
                    log.warn("Failed to notify booking service");
                }
            } catch (Exception e) {
                log.error("Exception notifying booking service: {}", e.getMessage());
            }
        }

        return getPaymentByOrderId(orderId);
    }

    /**
     * Overloaded method for backward compatibility
     */
    public PaymentResponseDto updatePaymentStatus(String orderId, PaymentStatus status,
            String transactionId, String responseCode, String responseMessage) {
        return updatePaymentStatus(orderId, status, transactionId, responseCode, responseMessage, null);
    }
}