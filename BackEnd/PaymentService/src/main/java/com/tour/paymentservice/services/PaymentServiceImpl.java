package com.tour.paymentservice.services;

import org.springframework.stereotype.Service;

import com.tour.paymentservice.dto.PaymentRequestDto;
import com.tour.paymentservice.dto.PaymentResponseDto;
import com.tour.paymentservice.entities.Payment;
import com.tour.paymentservice.entities.PaymentMethod;
import com.tour.paymentservice.entities.PaymentStatus;
import com.tour.paymentservice.repositories.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto request) {
        log.info("Creating payment request: orderId={}, method={}", request.getOrderId(), request.getPaymentMethod());

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
                log.info("Requested payment method {} differs from existing payment method {}, creating new payment",
                        request.getPaymentMethod(), latestPayment.getPaymentMethod());
                return createNewPayment(request);
            }

            // If payment is already completed or failed, create a new one
            if (latestPayment.getStatus() == PaymentStatus.COMPLETED ||
                    latestPayment.getStatus() == PaymentStatus.FAILED) {
                log.info("Existing payment is {}, creating new payment", latestPayment.getStatus());
                return createNewPayment(request);
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
                return createNewPayment(request);
            }
        }

        // No existing payment, create a new one
        log.info("No existing payment found, creating new payment");
        return createNewPayment(request);
    }

    private PaymentResponseDto createNewPayment(PaymentRequestDto request) {
        // Choose the payment gateway based on payment method
        if (request.getPaymentMethod() == PaymentMethod.MOMO) {
            return momoPaymentService.createMomoPayment(request);
        } else if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            return vnPayService.createVnPayPayment(request);
        }

        throw new IllegalArgumentException("Unsupported payment method: " + request.getPaymentMethod());
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
     * @return Updated payment response
     */
    public PaymentResponseDto updatePaymentStatus(String orderId, PaymentStatus status,
            String transactionId, String responseCode, String responseMessage) {

        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (payments.isEmpty()) {
            log.error("Payment not found for order ID: {}", orderId);
            return null;
        }

        // Get the most recent payment
        Payment payment = payments.get(0);

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
                boolean notified = bookingServiceClient.notifyPaymentCompletion(orderId, status.name());
                if (notified) {
                    log.info("Successfully notified booking service");
                } else {
                    log.warn("Failed to notify booking service");
                }
            } catch (Exception e) {
                log.error("Exception notifying booking service: {}", e.getMessage());
            }
        }

        return getPaymentByOrderId(orderId);
    }
}