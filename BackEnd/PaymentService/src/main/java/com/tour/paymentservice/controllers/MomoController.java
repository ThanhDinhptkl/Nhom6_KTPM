package com.tour.paymentservice.controllers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.paymentservice.dto.PaymentRequestDto;
import com.tour.paymentservice.dto.PaymentResponseDto;
import com.tour.paymentservice.entities.Payment;
import com.tour.paymentservice.entities.PaymentStatus;
import com.tour.paymentservice.entities.PaymentMethod;
import com.tour.paymentservice.repositories.PaymentRepository;
import com.tour.paymentservice.services.MomoPaymentService;
import com.tour.paymentservice.services.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller dedicated to handling MoMo payment requests
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/payments/momo")
@RequiredArgsConstructor
@Slf4j
public class MomoController {

    private final MomoPaymentService momoPaymentService;
    private final PaymentRepository paymentRepository;

    /**
     * Create a new MoMo payment
     * 
     * @param requestDto Payment request data
     * @return Payment response with URL to redirect user
     */
    @PostMapping("/create")
    public CompletableFuture<ResponseEntity<PaymentResponseDto>> createPayment(
            @RequestBody PaymentRequestDto requestDto) {
        log.info("Creating MoMo payment: {}", requestDto);
        return momoPaymentService.createMomoPayment(requestDto)
                .thenApply(responseDto -> ResponseEntity.ok(responseDto));
    }

    /**
     * Process MoMo payment callback
     * 
     * @param callbackData Callback data from MoMo
     * @return Updated payment status
     */
    @PostMapping("/callback")
    public ResponseEntity<String> processCallback(@RequestBody String callbackData) {
        log.info("Received MoMo callback: {}", callbackData);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(callbackData);

            String momoOrderId = root.path("orderId").asText();
            String errorCode = root.path("errorCode").asText();

            log.info("Processing MoMo callback for momoOrderId: {}, errorCode: {}", momoOrderId, errorCode);

            // Check if this is a versioned order ID (contains _v)
            String originalOrderId = momoOrderId;
            if (momoOrderId.contains("_v")) {
                // Extract the original order ID from extraData if possible
                String extraData = root.path("extraData").asText();
                if (extraData != null && extraData.contains("originalOrderId=")) {
                    String[] parts = extraData.split("originalOrderId=");
                    if (parts.length > 1) {
                        String part = parts[1];
                        int commaIndex = part.indexOf(",");
                        originalOrderId = commaIndex > 0 ? part.substring(0, commaIndex) : part;
                        log.info("Extracted original orderId {} from versioned orderId {}", originalOrderId,
                                momoOrderId);
                    }
                } else {
                    // If we can't extract from extraData, try to get the part before _v
                    int vIndex = momoOrderId.indexOf("_v");
                    if (vIndex > 0) {
                        originalOrderId = momoOrderId.substring(0, vIndex);
                        log.info("Extracted original orderId {} from versioned orderId {}", originalOrderId,
                                momoOrderId);
                    }
                }
            }

            // Find payment in database using the original orderId
            List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(originalOrderId);
            if (!payments.isEmpty()) {
                // Get the most recent payment
                Payment payment = payments.get(0);

                // Make sure it's a MOMO payment
                if (payment.getPaymentMethod() != PaymentMethod.MOMO) {
                    List<Payment> momoPayments = payments.stream()
                            .filter(p -> p.getPaymentMethod() == PaymentMethod.MOMO)
                            .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                            .toList();

                    if (!momoPayments.isEmpty()) {
                        payment = momoPayments.get(0);
                    }
                }

                // Update status
                if ("0".equals(errorCode)) {
                    payment.setStatus(PaymentStatus.COMPLETED);
                } else {
                    payment.setStatus(PaymentStatus.FAILED);
                }
                payment.setResponseCode(errorCode);
                payment.setResponseMessage(root.path("message").asText());
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                log.info("Updated payment status to {} for orderId: {}", payment.getStatus(), originalOrderId);
            } else {
                log.warn("Payment not found for orderId: {}", originalOrderId);
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing MoMo callback", e);
            return ResponseEntity.ok("Error");
        }
    }

    /**
     * Manual check for payment status - this can be called from the frontend after
     * user is redirected back
     * This helps when the callback wasn't received or processed correctly
     * 
     * @param orderId   The order ID to check
     * @param extraData Extra data from MoMo redirect (can contain resultCode)
     * @return Payment status
     */
    @GetMapping("/check-payment")
    public ResponseEntity<PaymentResponseDto> checkPayment(
            @RequestParam("orderId") String orderId,
            @RequestParam(value = "resultCode", required = false) String resultCode) {

        log.info("Manually checking MoMo payment for orderId: {}, resultCode: {}", orderId, resultCode);

        // Find payment in database
        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        if (payments.isEmpty()) {
            log.warn("Payment not found for orderId: {}", orderId);
            return ResponseEntity.notFound().build();
        }

        // Find MOMO payment
        Payment payment = null;
        for (Payment p : payments) {
            if (p.getPaymentMethod() == PaymentMethod.MOMO) {
                payment = p;
                break;
            }
        }

        if (payment == null) {
            log.warn("No MOMO payment found for orderId: {}", orderId);
            return ResponseEntity.notFound().build();
        }

        // If payment is still PENDING and we have a resultCode, update status
        if (payment.getStatus() == PaymentStatus.PENDING && resultCode != null) {
            if ("0".equals(resultCode)) {
                payment.setStatus(PaymentStatus.COMPLETED);
                log.info("Manually updating payment status to COMPLETED for orderId: {}", orderId);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                log.info("Manually updating payment status to FAILED for orderId: {}", orderId);
            }

            payment.setResponseCode(resultCode);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        // Return updated payment status
        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setOrderId(payment.getOrderId());
        responseDto.setTransactionId(payment.getTransactionId());
        responseDto.setAmount(payment.getAmount());
        responseDto.setStatus(payment.getStatus());
        responseDto.setPaymentMethod(payment.getPaymentMethod());
        responseDto.setResponseCode(payment.getResponseCode());
        responseDto.setResponseMessage(payment.getResponseMessage());
        responseDto.setPaymentUrl(payment.getPaymentUrl());

        return ResponseEntity.ok(responseDto);
    }
}