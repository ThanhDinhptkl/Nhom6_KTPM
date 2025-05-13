package com.tour.paymentservice.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import com.tour.paymentservice.dto.PaymentResponseDto;
import com.tour.paymentservice.entities.Payment;
import com.tour.paymentservice.entities.PaymentMethod;
import com.tour.paymentservice.entities.PaymentStatus;
import com.tour.paymentservice.repositories.PaymentRepository;
import com.tour.paymentservice.services.MomoPaymentService;
import com.tour.paymentservice.services.PaymentServiceImpl;
import com.tour.paymentservice.services.VnPayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller để xử lý các kết quả thanh toán chung từ nhiều cổng thanh toán
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/payments/result")
@RequiredArgsConstructor
@Slf4j
public class PaymentResultController {

    private final PaymentRepository paymentRepository;
    private final MomoPaymentService momoPaymentService;
    private final VnPayService vnPayService;
    private final PaymentServiceImpl paymentService;

    /**
     * Endpoint chung để kiểm tra trạng thái thanh toán từ các cổng thanh toán khác
     * nhau
     * 
     * @param orderId       ID của đơn hàng cần kiểm tra
     * @param paymentMethod Phương thức thanh toán (MOMO, VNPAY)
     * @param allParams     Tất cả các tham số từ URL callback/return
     * @return Thông tin thanh toán cập nhật
     */
    @GetMapping("/check")
    public ResponseEntity<PaymentResponseDto> checkPaymentResult(
            @RequestParam("orderId") String orderId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam Map<String, String> allParams) {

        log.info("Checking payment result for orderId: {}, method: {}, params: {}", orderId, paymentMethod, allParams);

        // Tìm thanh toán trong database
        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        if (payments.isEmpty()) {
            log.warn("Payment not found for orderId: {}", orderId);
            return ResponseEntity.notFound().build();
        }

        // Get the most recent payment
        Payment payment = payments.get(0);

        // Cập nhật trạng thái thanh toán dựa vào phương thức thanh toán
        if (payment.getStatus() == PaymentStatus.PENDING) {
            updatePaymentStatus(payment, allParams);
        }

        // Trả về thông tin thanh toán đã cập nhật
        PaymentResponseDto responseDto = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Endpoint để chuyển hướng người dùng đến trang payment-result với đầy đủ thông
     * tin
     */
    @GetMapping("/redirect")
    public RedirectView redirectToResultPage(
            @RequestParam("orderId") String orderId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam Map<String, String> allParams) {

        log.info("Redirecting to payment result page for orderId: {}, method: {}", orderId, paymentMethod);

        // Tìm thanh toán trong database
        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        if (payments.isEmpty()) {
            return new RedirectView("/payment-result.html?status=failure&message=Không+tìm+thấy+thông+tin+thanh+toán");
        }

        // Get the most recent payment
        Payment payment = payments.get(0);

        // Cập nhật trạng thái thanh toán nếu cần
        updatePaymentStatus(payment, allParams);

        // Tạo URL redirect đến trang kết quả
        StringBuilder url = new StringBuilder("/payment-result.html?");
        url.append("status=").append(payment.getStatus() == PaymentStatus.COMPLETED ? "success" : "failure");
        url.append("&transactionId=").append(payment.getTransactionId() != null ? payment.getTransactionId() : "");
        url.append("&amount=").append(payment.getAmount());
        url.append("&paymentMethod=").append(payment.getPaymentMethod());
        url.append("&message=").append(
                payment.getStatus() == PaymentStatus.COMPLETED ? "Thanh+toán+thành+công" : "Thanh+toán+thất+bại");
        url.append("&time=").append(
                payment.getUpdatedAt() != null ? payment.getUpdatedAt().toString() : LocalDateTime.now().toString());

        return new RedirectView(url.toString());
    }

    private void updatePaymentStatus(Payment payment, Map<String, String> params) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return; // Không cập nhật nếu đã hoàn thành hoặc thất bại
        }

        String transactionId = null;
        String responseCode = null;
        String responseMessage = null;
        PaymentStatus newStatus = null;

        if (payment.getPaymentMethod() == PaymentMethod.MOMO) {
            String errorCode = params.get("errorCode");
            if (errorCode != null) {
                newStatus = "0".equals(errorCode) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
                responseCode = errorCode;
                transactionId = params.get("transId");
                responseMessage = "0".equals(errorCode) ? "Thanh toán thành công" : "Thanh toán thất bại";
            }
        } else if (payment.getPaymentMethod() == PaymentMethod.VNPAY) {
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            if (vnp_ResponseCode != null) {
                newStatus = "00".equals(vnp_ResponseCode) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
                responseCode = vnp_ResponseCode;
                transactionId = params.get("vnp_TransactionNo");
                responseMessage = "00".equals(vnp_ResponseCode) ? "Thanh toán thành công" : "Thanh toán thất bại";
            }
        }

        if (newStatus != null) {
            // Sử dụng phương thức cập nhật mới, sẽ thông báo cho booking service
            paymentService.updatePaymentStatus(
                    payment.getOrderId(),
                    newStatus,
                    transactionId,
                    responseCode,
                    responseMessage);
        }
    }
}