package com.tour.paymentservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.paymentservice.dto.PaymentRequestDto;
import com.tour.paymentservice.dto.PaymentResponseDto;
import com.tour.paymentservice.entities.Payment;
import com.tour.paymentservice.entities.PaymentStatus;
import com.tour.paymentservice.repositories.PaymentRepository;
import com.tour.paymentservice.services.MomoPaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Controller dedicated to handling MoMo payment requests
 */
@RestController
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
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto requestDto) {
        log.info("Creating MoMo payment: {}", requestDto);
        PaymentResponseDto responseDto = momoPaymentService.createMomoPayment(requestDto);
        return ResponseEntity.ok(responseDto);
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

            String orderId = root.path("orderId").asText();
            String errorCode = root.path("errorCode").asText();

            log.info("Processing MoMo callback for orderId: {}, errorCode: {}", orderId, errorCode);

            // Tìm payment trong database
            Payment payment = paymentRepository.findByOrderId(orderId);
            if (payment != null) {
                // Cập nhật trạng thái
                if ("0".equals(errorCode)) {
                    payment.setStatus(PaymentStatus.COMPLETED);
                } else {
                    payment.setStatus(PaymentStatus.FAILED);
                }
                payment.setResponseCode(errorCode);
                payment.setResponseMessage(root.path("message").asText());
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                log.info("Updated payment status to {} for orderId: {}", payment.getStatus(), orderId);
            } else {
                log.warn("Payment not found for orderId: {}", orderId);
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
        Payment payment = paymentRepository.findByOrderId(orderId);

        if (payment == null) {
            log.warn("Payment not found for orderId: {}", orderId);
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