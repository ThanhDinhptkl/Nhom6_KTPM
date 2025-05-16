package com.tour.paymentservice.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.paymentservice.config.PaymentConfig;
import com.tour.paymentservice.dto.PaymentRequestDto;
import com.tour.paymentservice.dto.PaymentResponseDto;
import com.tour.paymentservice.entities.Payment;
import com.tour.paymentservice.entities.PaymentMethod;
import com.tour.paymentservice.entities.PaymentStatus;
import com.tour.paymentservice.repositories.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomoPaymentService {

    private final PaymentConfig paymentConfig;
    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Create a dedicated HttpClient instance
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Create MOMO payment with retry and time limiter for API calls
     */
    @TimeLimiter(name = "momoPayment")
    @Retry(name = "momoPayment")
    public CompletableFuture<PaymentResponseDto> createMomoPayment(PaymentRequestDto requestDto) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating MoMo payment for orderId: {}, amount: {}",
                        requestDto.getOrderId(), requestDto.getAmount());

                // Check if there's an existing MOMO payment for this orderId
                List<Payment> existingMomoPayments = paymentRepository
                        .findByOrderIdOrderByCreatedAtDesc(requestDto.getOrderId());

                // Create a versioned orderId if needed to avoid "OrderId exists" error
                String originalOrderId = requestDto.getOrderId();
                String versionedOrderId = originalOrderId;

                // Filter only MOMO payments
                List<Payment> momoPayments = existingMomoPayments.stream()
                        .filter(p -> p.getPaymentMethod() == PaymentMethod.MOMO)
                        .toList();

                if (!momoPayments.isEmpty()) {
                    // If we have previous MOMO payments, check if the latest one can be reused
                    Payment latestMomoPayment = momoPayments.get(0);

                    // Check if payment is pending and not expired (within 15 minutes)
                    boolean isPending = latestMomoPayment.getStatus() == PaymentStatus.PENDING;
                    boolean isNotExpired = latestMomoPayment.getCreatedAt().plusMinutes(15)
                            .isAfter(LocalDateTime.now());

                    if (isPending && isNotExpired && latestMomoPayment.getPaymentUrl() != null) {
                        // If the payment is still valid, return the existing payment
                        log.info("Reusing existing valid MOMO payment for orderId: {}", originalOrderId);
                        return modelMapper.map(latestMomoPayment, PaymentResponseDto.class);
                    } else {
                        // Create a versioned orderId (original_v1, original_v2, etc.)
                        int version = 1;
                        for (Payment p : momoPayments) {
                            if (p.getOrderId().startsWith(originalOrderId + "_v")) {
                                try {
                                    String vStr = p.getOrderId().substring(originalOrderId.length() + 2);
                                    int v = Integer.parseInt(vStr);
                                    version = Math.max(version, v + 1);
                                } catch (Exception e) {
                                    // Ignore parsing errors, just increment version
                                    version++;
                                }
                            }
                        }

                        versionedOrderId = originalOrderId + "_v" + version;
                        log.info("Created versioned orderId: {} for MOMO payment", versionedOrderId);
                    }
                }

                // Use versioned orderId for MOMO API request, but keep tracking original
                // orderId
                String transactionId = UUID.randomUUID().toString();
                String requestId = transactionId;

                // Xây dựng returnUrl đúng format
                String returnUrl = paymentConfig.getMomoReturnUrl();
                if (returnUrl == null || returnUrl.isEmpty()) {
                    returnUrl = paymentConfig.getServerBaseUrl() + "/api/payments/momo/return";
                }

                // Thêm orderId vào returnUrl
                returnUrl += (returnUrl.contains("?") ? "&" : "?") + "orderId=" + requestDto.getOrderId();

                log.info("Using returnUrl for Momo: {}", returnUrl);

                // Build request body map - matching the exact format from MoMo documentation
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("partnerCode", paymentConfig.getMomoPartnerCode());
                requestBody.put("accessKey", paymentConfig.getMomoAccessKey());
                requestBody.put("requestId", requestId);
                requestBody.put("amount", String.valueOf(requestDto.getAmount().longValue()));
                requestBody.put("orderId", versionedOrderId);
                requestBody.put("orderInfo", requestDto.getDescription());
                requestBody.put("returnUrl", returnUrl);
                requestBody.put("notifyUrl", paymentConfig.getMomoCallbackUrl());
                requestBody.put("requestType", "captureMoMoWallet");
                requestBody.put("extraData",
                        "email=" + (requestDto.getCustomerEmail() != null ? requestDto.getCustomerEmail() : "") +
                                ",originalOrderId=" + originalOrderId);

                // Generate signature for the request - exactly as in documentation
                String rawSignature = "partnerCode=" + paymentConfig.getMomoPartnerCode() +
                        "&accessKey=" + paymentConfig.getMomoAccessKey() +
                        "&requestId=" + requestId +
                        "&amount=" + String.valueOf(requestDto.getAmount().longValue()) +
                        "&orderId=" + versionedOrderId +
                        "&orderInfo=" + requestDto.getDescription() +
                        "&returnUrl=" + returnUrl +
                        "&notifyUrl=" + paymentConfig.getMomoCallbackUrl() +
                        "&extraData=" + requestBody.get("extraData");

                log.debug("Raw signature string: {}", rawSignature);

                // Create HMAC SHA256 signature
                String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
                        paymentConfig.getMomoSecretKey()).hmacHex(rawSignature);

                log.debug("Generated signature: {}", signature);
                requestBody.put("signature", signature);

                // Convert request body to JSON
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                log.info("Sending request to MoMo: {}", jsonBody);

                // Make direct HTTP call to MoMo API using Java's HttpClient
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(paymentConfig.getMomoPaymentUrl()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                // Send the request and get the response
                HttpResponse<String> response;
                try {
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    log.info("MoMo API response status: {}", response.statusCode());
                    log.info("MoMo API response body: {}", response.body());
                } catch (IOException | InterruptedException e) {
                    log.error("Error communicating with MoMo API: {}", e.getMessage());
                    throw new RuntimeException("Failed to connect to MoMo payment service", e);
                }

                // Parse response
                Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);

                // Check if the response is successful
                Object errorCodeObj = responseMap.get("errorCode");
                String errorCode = (errorCodeObj != null) ? String.valueOf(errorCodeObj) : "ERROR";
                boolean isSuccessful = "0".equals(errorCode);

                // Save payment to database
                Payment payment = Payment.builder()
                        .orderId(originalOrderId) // Keep the original orderId for lookup
                        .transactionId(transactionId)
                        .amount(requestDto.getAmount())
                        .paymentMethod(PaymentMethod.MOMO)
                        .status(isSuccessful ? PaymentStatus.PENDING : PaymentStatus.FAILED)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .paymentUrl((String) responseMap.get("payUrl"))
                        .responseCode(errorCode)
                        .responseMessage((String) responseMap.get("message"))
                        .customerEmail(requestDto.getCustomerEmail())
                        .description(requestDto.getDescription())
                        .build();

                // Store the versioned orderId in description field if different from original
                if (!versionedOrderId.equals(originalOrderId)) {
                    payment.setDescription(payment.getDescription() + " (MOMO orderId: " + versionedOrderId + ")");
                }

                payment = paymentRepository.save(payment);
                log.info("Saved MoMo payment in database with id: {}, status: {}", payment.getId(),
                        payment.getStatus());

                // Map to response DTO
                PaymentResponseDto responseDto = modelMapper.map(payment, PaymentResponseDto.class);
                return responseDto;

            } catch (Exception e) {
                log.error("Error creating MoMo payment: {}", e.getMessage(), e);

                // Create failed payment record
                Payment payment = Payment.builder()
                        .orderId(requestDto.getOrderId())
                        .transactionId(UUID.randomUUID().toString())
                        .amount(requestDto.getAmount())
                        .paymentMethod(PaymentMethod.MOMO)
                        .status(PaymentStatus.FAILED)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .responseCode("ERROR")
                        .responseMessage(e.getMessage())
                        .customerEmail(requestDto.getCustomerEmail())
                        .description(requestDto.getDescription())
                        .build();

                paymentRepository.save(payment);

                // Map to response DTO
                PaymentResponseDto responseDto = modelMapper.map(payment, PaymentResponseDto.class);
                throw new CompletionException(e);
            }
        });
    }

    public PaymentResponseDto processMomoCallback(String requestData) {
        try {
            log.info("Processing MoMo callback: {}", requestData);
            Map<String, Object> callbackData = objectMapper.readValue(requestData, Map.class);

            String momoOrderId = (String) callbackData.get("orderId");
            String extraData = (String) callbackData.get("extraData");

            // Handle resultCode safely as it could be an Integer
            Object resultCodeObj = callbackData.get("resultCode");
            String resultCode = (resultCodeObj != null) ? String.valueOf(resultCodeObj) : "ERROR";

            log.info("MoMo callback for momoOrderId: {}, resultCode: {}", momoOrderId, resultCode);

            // Extract original orderId if this is a versioned orderId
            String originalOrderId = momoOrderId;
            if (momoOrderId.contains("_v")) {
                // Try to extract from extraData first
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
                    // If not available in extraData, try to parse it from the orderId
                    int vIndex = momoOrderId.indexOf("_v");
                    if (vIndex > 0) {
                        originalOrderId = momoOrderId.substring(0, vIndex);
                        log.info("Extracted original orderId {} from versioned orderId {}", originalOrderId,
                                momoOrderId);
                    }
                }
            }

            // Find the payment by orderId using the original orderId
            List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(originalOrderId);

            if (!payments.isEmpty()) {
                // Get most recent payment
                Payment payment = payments.get(0);

                // Make sure we're getting a MOMO payment
                if (payment.getPaymentMethod() != PaymentMethod.MOMO) {
                    List<Payment> momoPayments = payments.stream()
                            .filter(p -> p.getPaymentMethod() == PaymentMethod.MOMO)
                            .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                            .toList();

                    if (!momoPayments.isEmpty()) {
                        payment = momoPayments.get(0);
                        log.info("Selected MOMO payment from multiple payments for orderId: {}", originalOrderId);
                    } else {
                        log.warn("No MOMO payment found for orderId: {}", originalOrderId);
                        return null;
                    }
                }

                PaymentStatus newStatus;
                // Update payment status
                if ("0".equals(resultCode)) {
                    newStatus = PaymentStatus.COMPLETED;
                    log.info("Setting MoMo payment status to COMPLETED for orderId: {}", originalOrderId);
                } else {
                    newStatus = PaymentStatus.FAILED;
                    log.info("Setting MoMo payment status to FAILED for orderId: {}", originalOrderId);
                }

                // Get transaction ID from MoMo if provided
                String transId = (String) callbackData.get("transId");
                String transactionId = (transId != null && !transId.isEmpty()) ? transId : payment.getTransactionId();

                // Use paymentService to update status and notify booking service
                // We need to access the PaymentServiceImpl instance, so inject it
                PaymentService paymentService = applicationContext.getBean(PaymentService.class);
                PaymentResponseDto responseDto = paymentService.updatePaymentStatus(
                        originalOrderId,
                        newStatus,
                        transactionId,
                        resultCode,
                        (String) callbackData.get("message"),
                        PaymentMethod.MOMO);

                log.info("Updated MoMo payment status to {} and notified booking service", newStatus);
                return responseDto;
            } else {
                log.warn("Payment not found for MoMo callback with orderId: {}", originalOrderId);
                return null;
            }
        } catch (Exception e) {
            log.error("Error processing MoMo callback: {}", e.getMessage(), e);
            return null;
        }
    }
}