package com.tour.paymentservice.controllers;

import com.tour.paymentservice.entities.Payment;
import com.tour.paymentservice.entities.PaymentStatus;
import com.tour.paymentservice.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import com.tour.paymentservice.dto.PaymentRequestDto;
import com.tour.paymentservice.dto.PaymentResponseDto;
import com.tour.paymentservice.entities.PaymentMethod;
import com.tour.paymentservice.services.MomoPaymentService;
import com.tour.paymentservice.services.PaymentService;
import com.tour.paymentservice.services.VnPayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.modelmapper.ModelMapper;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final VnPayService vnPayService;
    private final MomoPaymentService momoPaymentService;
    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;

    @Value("${app.server.base-url}")
    private String serverBaseUrl;

    @PostMapping
    public CompletableFuture<ResponseEntity<PaymentResponseDto>> createPayment(
            @RequestBody PaymentRequestDto requestDto) {
        log.info("Creating payment with method: {}, orderId: {}", requestDto.getPaymentMethod(),
                requestDto.getOrderId());

        // Thiết lập orderId nếu không có
        if (requestDto.getOrderId() == null || requestDto.getOrderId().trim().isEmpty()) {
            requestDto.setOrderId("ORDER-" + UUID.randomUUID().toString());
        }

        try {
            // Thiết lập returnUrl để trỏ về API redirect của chúng ta
            String returnUrl = serverBaseUrl + "/api/payments/result/redirect"
                    + "?orderId=" + URLEncoder.encode(requestDto.getOrderId(), StandardCharsets.UTF_8.toString())
                    + "&paymentMethod=" + requestDto.getPaymentMethod();

            requestDto.setReturnUrl(returnUrl);
            log.info("Set return URL: {}", returnUrl);
        } catch (Exception e) {
            log.error("Error encoding URL", e);
        }

        return paymentService.createPayment(requestDto)
                .thenApply(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponseDto> getPaymentStatus(
            @PathVariable String orderId,
            @RequestParam(value = "paymentMethod", required = false) PaymentMethod paymentMethod) {

        log.info("Getting payment status for order: {}, paymentMethod: {}", orderId, paymentMethod);

        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (payments.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Payment payment = null;

        if (paymentMethod != null) {
            // Filter for specific payment method
            for (Payment p : payments) {
                if (p.getPaymentMethod() == paymentMethod) {
                    payment = p;
                    break;
                }
            }

            if (payment == null) {
                log.warn("No payment found with method {} for orderID: {}", paymentMethod, orderId);
                return ResponseEntity.notFound().build();
            }
        } else {
            // Default to most recent payment if no method specified
            payment = payments.get(0);
        }

        // Map to response DTO
        PaymentResponseDto responseDto = modelMapper.map(payment, PaymentResponseDto.class);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/vnpay/callback")
    public RedirectView processVnPayCallback(@RequestParam Map<String, String> callbackParams) {
        log.info("VNPay callback received: {}", callbackParams);

        String vnp_ResponseCode = callbackParams.get("vnp_ResponseCode");
        String vnp_TxnRef = callbackParams.get("vnp_TxnRef"); // Mã đơn hàng

        if (vnp_ResponseCode == null) {
            return new RedirectView("/payment-result?status=failure&message=Thiếu mã phản hồi");
        }

        PaymentResponseDto response = vnPayService.processVnPayCallback(callbackParams);
        log.info("VNPay processing result: {}", response);

        if (response == null) {
            return new RedirectView("/payment-result?status=failure&message=Không thể xử lý thanh toán");
        }

        // Xây dựng URL chuyển hướng đến trang payment-result
        StringBuilder redirectUrl = new StringBuilder("/payment-result?");

        // Thêm thông tin trạng thái
        boolean isSuccess = "00".equals(vnp_ResponseCode);
        redirectUrl.append("status=").append(isSuccess ? "success" : "failure");

        // Thêm các thông tin khác
        redirectUrl.append("&transactionId=")
                .append(response.getTransactionId() != null ? response.getTransactionId() : "");
        redirectUrl.append("&amount=").append(response.getAmount());
        redirectUrl.append("&paymentMethod=").append(response.getPaymentMethod());
        redirectUrl.append("&message=").append(isSuccess ? "Thanh toán thành công" : "Thanh toán thất bại");
        redirectUrl.append("&time=").append(java.time.LocalDateTime.now());

        return new RedirectView(redirectUrl.toString());
    }

    @GetMapping("/test/momo")
    public CompletableFuture<ResponseEntity<PaymentResponseDto>> testMomoPayment(
            @RequestParam(defaultValue = "10000") BigDecimal amount) {
        log.info("Testing MoMo payment with amount: {}", amount);

        // Create a test payment request
        PaymentRequestDto requestDto = PaymentRequestDto.builder()
                .orderId("TEST-" + UUID.randomUUID().toString())
                .amount(amount)
                .description("Test payment with MoMo")
                .customerEmail("test@example.com")
                .returnUrl("http://localhost:8085/payment-result")
                .paymentMethod(PaymentMethod.MOMO)
                .build();

        return paymentService.createPayment(requestDto)
                .thenApply(response -> ResponseEntity.ok(response));
    }

    /**
     * Endpoint chuyển hướng đến trang kết quả thanh toán
     */
    @GetMapping("/redirect")
    public RedirectView redirectToPaymentResultPage(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam Map<String, String> allParams) {

        log.info("Redirecting to payment result page: {}", allParams);

        if (orderId == null) {
            // Nếu không có orderId, chuyển hướng đến trang kết quả với thông báo lỗi
            return new RedirectView("/payment-result?status=failure&message=Không tìm thấy thông tin thanh toán");
        }

        // Chuyển hướng đến endpoint xử lý kết quả thanh toán
        StringBuilder redirectUrl = new StringBuilder("/api/payments/result/redirect?orderId=").append(orderId);

        if (paymentMethod != null) {
            redirectUrl.append("&paymentMethod=").append(paymentMethod);
        }

        // Thêm các tham số khác từ request
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("orderId") && !key.equals("paymentMethod")) {
                redirectUrl.append("&").append(key).append("=").append(entry.getValue());
            }
        }

        return new RedirectView(redirectUrl.toString());
    }

    /**
     * Endpoint xử lý chuyển hướng sau khi thanh toán Momo
     */
    @GetMapping("/momo/return")
    public RedirectView processMomoReturn(@RequestParam Map<String, String> returnParams) {
        log.info("Momo return URL accessed with params: {}", returnParams);

        String momoOrderId = returnParams.get("orderId");
        String errorCode = returnParams.get("errorCode");
        String extraData = returnParams.get("extraData");

        log.info("Processing Momo return for momoOrderId={}, errorCode={}", momoOrderId, errorCode);

        if (momoOrderId == null) {
            return new RedirectView("/payment-result.html?status=failure&message=Không+tìm+thấy+mã+đơn+hàng");
        }

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
                    log.info("Extracted original orderId {} from versioned orderId {}", originalOrderId, momoOrderId);
                }
            } else {
                // If not available in extraData, try to parse it from the orderId
                int vIndex = momoOrderId.indexOf("_v");
                if (vIndex > 0) {
                    originalOrderId = momoOrderId.substring(0, vIndex);
                    log.info("Extracted original orderId {} from versioned orderId {}", originalOrderId, momoOrderId);
                }
            }
        }

        // Update the orderId parameter for client redirect to use the original order ID
        returnParams.put("orderId", originalOrderId);

        // Find payment in database using the original orderId
        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(originalOrderId);
        if (payments.isEmpty()) {
            return new RedirectView("/payment-result.html?status=failure&message=Không+tìm+thấy+thông+tin+thanh+toán");
        }

        // Get the most recent payment - make sure it's a MOMO payment
        Payment payment = payments.get(0);
        if (payment.getPaymentMethod() != PaymentMethod.MOMO) {
            List<Payment> momoPayments = payments.stream()
                    .filter(p -> p.getPaymentMethod() == PaymentMethod.MOMO)
                    .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                    .toList();

            if (!momoPayments.isEmpty()) {
                payment = momoPayments.get(0);
            }
        }

        // Update payment status if not already updated
        if (payment.getStatus() == PaymentStatus.PENDING && errorCode != null) {
            boolean isSuccess = "0".equals(errorCode);
            PaymentStatus newStatus = isSuccess ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;

            // Get transaction ID from MoMo if available
            String transId = returnParams.get("transId");

            // Use paymentService to update status and notify booking service
            PaymentResponseDto updatedPayment = paymentService.updatePaymentStatus(
                    originalOrderId,
                    newStatus,
                    transId != null ? transId : payment.getTransactionId(),
                    errorCode,
                    isSuccess ? "Payment completed" : "Payment failed",
                    PaymentMethod.MOMO);

            log.info("Updated payment status to {} for orderId: {}", newStatus, originalOrderId);
        }

        // Build redirect URL with payment information
        StringBuilder url = new StringBuilder("/payment/callback?");
        boolean isSuccess = payment.getStatus() == PaymentStatus.COMPLETED;
        url.append("status=").append(isSuccess ? "success" : "failure");
        url.append("&transactionId=").append(payment.getTransactionId() != null ? payment.getTransactionId() : "");
        url.append("&amount=").append(payment.getAmount());
        url.append("&paymentMethod=").append(payment.getPaymentMethod());
        url.append("&message=").append(isSuccess ? "Thanh+toán+thành+công" : "Thanh+toán+thất+bại");
        url.append("&time=").append(payment.getUpdatedAt().toString());

        return new RedirectView(url.toString());
    }

    /**
     * Webhook endpoint for MoMo IPN (Instant Payment Notification)
     */
    @PostMapping("/momo/ipn")
    @ResponseBody
    public String processMomoIPN(@RequestBody Map<String, Object> ipnData) {
        log.info("Received MoMo IPN webhook: {}", ipnData);

        // Get orderId and resultCode from IPN data
        String momoOrderId = (String) ipnData.get("orderId");
        Object resultCodeObj = ipnData.get("resultCode");
        String resultCode = (resultCodeObj != null) ? String.valueOf(resultCodeObj) : "ERROR";
        String extraData = (String) ipnData.get("extraData");

        if (momoOrderId == null) {
            log.error("MoMo IPN missing orderId");
            return "FAIL";
        }

        log.info("Processing MoMo IPN for momoOrderId={}, resultCode={}", momoOrderId, resultCode);

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
                    log.info("Extracted original orderId {} from versioned orderId {}", originalOrderId, momoOrderId);
                }
            } else {
                // If not available in extraData, try to parse it from the orderId
                int vIndex = momoOrderId.indexOf("_v");
                if (vIndex > 0) {
                    originalOrderId = momoOrderId.substring(0, vIndex);
                    log.info("Extracted original orderId {} from versioned orderId {}", originalOrderId, momoOrderId);
                }
            }
        }

        try {
            // Find payment by orderId
            List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(originalOrderId);
            if (payments.isEmpty()) {
                log.error("Payment not found for orderId: {}", originalOrderId);
                return "FAIL";
            }

            Payment payment = payments.get(0);

            // Make sure it's a MOMO payment
            if (payment.getPaymentMethod() != PaymentMethod.MOMO) {
                List<Payment> momoPayments = payments.stream()
                        .filter(p -> p.getPaymentMethod() == PaymentMethod.MOMO)
                        .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                        .toList();

                if (!momoPayments.isEmpty()) {
                    payment = momoPayments.get(0);
                } else {
                    log.error("No MOMO payment found for orderId: {}", originalOrderId);
                    return "FAIL";
                }
            }

            // Only update if payment is still PENDING
            if (payment.getStatus() == PaymentStatus.PENDING) {
                PaymentStatus newStatus;
                if ("0".equals(resultCode)) {
                    newStatus = PaymentStatus.COMPLETED;
                } else {
                    newStatus = PaymentStatus.FAILED;
                }

                // Get transaction ID if available
                String transId = (String) ipnData.get("transId");

                // Update payment status
                PaymentResponseDto result = paymentService.updatePaymentStatus(
                        originalOrderId,
                        newStatus,
                        transId != null ? transId : payment.getTransactionId(),
                        resultCode,
                        (String) ipnData.get("message"),
                        PaymentMethod.MOMO);

                log.info("Successfully updated payment status from MoMo IPN: {}", newStatus);
            } else {
                log.info("Payment already in final state: {}, no update needed", payment.getStatus());
            }

            // Return success to MoMo
            return "OK";
        } catch (Exception e) {
            log.error("Error processing MoMo IPN: {}", e.getMessage(), e);
            return "FAIL";
        }
    }
}