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

    public PaymentResponseDto createMomoPayment(PaymentRequestDto requestDto) {
        try {
            log.info("Creating MoMo payment for orderId: {}, amount: {}",
                    requestDto.getOrderId(), requestDto.getAmount());

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
            requestBody.put("orderId", requestDto.getOrderId());
            requestBody.put("orderInfo", requestDto.getDescription());
            requestBody.put("returnUrl", returnUrl);
            requestBody.put("notifyUrl", paymentConfig.getMomoCallbackUrl());
            requestBody.put("requestType", "captureMoMoWallet");
            requestBody.put("extraData",
                    "email=" + (requestDto.getCustomerEmail() != null ? requestDto.getCustomerEmail() : ""));

            // Generate signature for the request - exactly as in documentation
            String rawSignature = "partnerCode=" + paymentConfig.getMomoPartnerCode() +
                    "&accessKey=" + paymentConfig.getMomoAccessKey() +
                    "&requestId=" + requestId +
                    "&amount=" + String.valueOf(requestDto.getAmount().longValue()) +
                    "&orderId=" + requestDto.getOrderId() +
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
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("MoMo API response status: {}", response.statusCode());
            log.info("MoMo API response body: {}", response.body());

            // Parse response
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);

            // Check if the response is successful
            Object errorCodeObj = responseMap.get("errorCode");
            String errorCode = (errorCodeObj != null) ? String.valueOf(errorCodeObj) : "ERROR";
            boolean isSuccessful = "0".equals(errorCode);

            // Save payment to database
            Payment payment = Payment.builder()
                    .orderId(requestDto.getOrderId())
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

            payment = paymentRepository.save(payment);
            log.info("Saved MoMo payment in database with id: {}, status: {}", payment.getId(), payment.getStatus());

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
            return responseDto;
        }
    }

    public PaymentResponseDto processMomoCallback(String requestData) {
        try {
            log.info("Processing MoMo callback: {}", requestData);
            Map<String, Object> callbackData = objectMapper.readValue(requestData, Map.class);

            String orderId = (String) callbackData.get("orderId");

            // Handle resultCode safely as it could be an Integer
            Object resultCodeObj = callbackData.get("resultCode");
            String resultCode = (resultCodeObj != null) ? String.valueOf(resultCodeObj) : "ERROR";

            log.info("MoMo callback for orderId: {}, resultCode: {}", orderId, resultCode);

            // Find the payment by orderId
            List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

            if (!payments.isEmpty()) {
                // Get most recent payment
                Payment payment = payments.get(0);

                PaymentStatus newStatus;
                // Update payment status
                if ("0".equals(resultCode)) {
                    newStatus = PaymentStatus.COMPLETED;
                    log.info("Setting MoMo payment status to COMPLETED for orderId: {}", orderId);
                } else {
                    newStatus = PaymentStatus.FAILED;
                    log.info("Setting MoMo payment status to FAILED for orderId: {}", orderId);
                }

                // Get transaction ID from MoMo if provided
                String transId = (String) callbackData.get("transId");
                String transactionId = (transId != null && !transId.isEmpty()) ? transId : payment.getTransactionId();

                // Use paymentService to update status and notify booking service
                // We need to access the PaymentServiceImpl instance, so inject it
                PaymentService paymentService = applicationContext.getBean(PaymentService.class);
                PaymentResponseDto responseDto = paymentService.updatePaymentStatus(
                        orderId,
                        newStatus,
                        transactionId,
                        resultCode,
                        (String) callbackData.get("message"));

                log.info("Updated MoMo payment status to {} and notified booking service", newStatus);
                return responseDto;
            } else {
                log.warn("Payment not found for MoMo callback with orderId: {}", orderId);
                return null;
            }
        } catch (Exception e) {
            log.error("Error processing MoMo callback: {}", e.getMessage(), e);
            return null;
        }
    }
}