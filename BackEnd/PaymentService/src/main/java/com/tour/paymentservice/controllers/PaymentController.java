package com.tour.paymentservice.controllers;

import com.tour.paymentservice.entities.Payment;
import com.tour.paymentservice.entities.PaymentStatus;
import com.tour.paymentservice.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final VnPayService vnPayService;
    private final MomoPaymentService momoPaymentService;
    private final PaymentRepository paymentRepository;

    @Value("${app.server.base-url}")
    private String serverBaseUrl;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto requestDto) {
        log.info("Creating payment with method: {}", requestDto.getPaymentMethod());

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

        PaymentResponseDto response = paymentService.createPayment(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponseDto> getPaymentStatus(@PathVariable String orderId) {
        log.info("Getting payment status for order: {}", orderId);

        PaymentResponseDto response = paymentService.getPaymentByOrderId(orderId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
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
    public ResponseEntity<PaymentResponseDto> testMomoPayment(@RequestParam(defaultValue = "10000") BigDecimal amount) {
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

        PaymentResponseDto response = paymentService.createPayment(requestDto);
        return ResponseEntity.ok(response);
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

        String orderId = returnParams.get("orderId");
        String errorCode = returnParams.get("errorCode");

        log.info("Processing Momo return for orderId={}, errorCode={}", orderId, errorCode);

        if (orderId == null) {
            return new RedirectView("/payment-result.html?status=failure&message=Không+tìm+thấy+mã+đơn+hàng");
        }

        // Tìm thanh toán trong database
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            return new RedirectView("/payment-result.html?status=failure&message=Không+tìm+thấy+thông+tin+thanh+toán");
        }

        // Cập nhật trạng thái thanh toán nếu chưa được cập nhật
        if (payment.getStatus() == PaymentStatus.PENDING && errorCode != null) {
            boolean isSuccess = "0".equals(errorCode);
            payment.setStatus(isSuccess ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
            payment.setResponseCode(errorCode);
            payment.setUpdatedAt(LocalDateTime.now());

            // Lưu transaction ID từ MoMo nếu có
            String transId = returnParams.get("transId");
            if (transId != null && !transId.isEmpty()) {
                payment.setTransactionId(transId);
            }

            paymentRepository.save(payment);
            log.info("Updated payment status to {} for orderId: {}", payment.getStatus(), orderId);
        }

        // Xây dựng URL redirect với đầy đủ thông tin
        StringBuilder url = new StringBuilder("/payment-result.html?");
        boolean isSuccess = payment.getStatus() == PaymentStatus.COMPLETED;
        url.append("status=").append(isSuccess ? "success" : "failure");
        url.append("&transactionId=").append(payment.getTransactionId() != null ? payment.getTransactionId() : "");
        url.append("&amount=").append(payment.getAmount());
        url.append("&paymentMethod=").append(payment.getPaymentMethod());
        url.append("&message=").append(isSuccess ? "Thanh+toán+thành+công" : "Thanh+toán+thất+bại");
        url.append("&time=").append(payment.getUpdatedAt().toString());

        return new RedirectView(url.toString());
    }
}