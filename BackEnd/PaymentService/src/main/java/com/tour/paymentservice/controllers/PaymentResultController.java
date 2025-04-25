package com.tour.paymentservice.controllers;

import java.time.LocalDateTime;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.tour.paymentservice.dto.PaymentResponseDto;
import com.tour.paymentservice.entities.Payment;
import com.tour.paymentservice.entities.PaymentMethod;
import com.tour.paymentservice.entities.PaymentStatus;
import com.tour.paymentservice.repositories.PaymentRepository;
import com.tour.paymentservice.services.MomoPaymentService;
import com.tour.paymentservice.services.VnPayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller để xử lý các kết quả thanh toán chung từ nhiều cổng thanh toán
 */
@RestController
@RequestMapping("/api/payments/result")
@RequiredArgsConstructor
@Slf4j
public class PaymentResultController {

    private final PaymentRepository paymentRepository;
    private final MomoPaymentService momoPaymentService;
    private final VnPayService vnPayService;

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
        Payment payment = paymentRepository.findByOrderId(orderId);

        if (payment == null) {
            log.warn("Payment not found for orderId: {}", orderId);
            return ResponseEntity.notFound().build();
        }

        // Cập nhật trạng thái thanh toán dựa vào phương thức thanh toán
        if (payment.getStatus() == PaymentStatus.PENDING) {
            if (payment.getPaymentMethod() == PaymentMethod.MOMO) {
                // Xử lý kết quả từ MoMo
                String resultCode = allParams.get("resultCode");
                if (resultCode != null) {
                    if ("0".equals(resultCode)) {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        log.info("Updating MoMo payment status to COMPLETED for orderId: {}", orderId);
                    } else {
                        payment.setStatus(PaymentStatus.FAILED);
                        log.info("Updating MoMo payment status to FAILED for orderId: {}", orderId);
                    }

                    payment.setResponseCode(resultCode);
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
            } else if (payment.getPaymentMethod() == PaymentMethod.VNPAY) {
                // Xử lý kết quả từ VNPay
                String vnp_ResponseCode = allParams.get("vnp_ResponseCode");
                if (vnp_ResponseCode != null) {
                    if ("00".equals(vnp_ResponseCode)) {
                        payment.setStatus(PaymentStatus.COMPLETED);
                        log.info("Updating VNPay payment status to COMPLETED for orderId: {}", orderId);
                    } else {
                        payment.setStatus(PaymentStatus.FAILED);
                        log.info("Updating VNPay payment status to FAILED for orderId: {}", orderId);
                    }

                    payment.setResponseCode(vnp_ResponseCode);
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
            }
        }

        // Trả về thông tin thanh toán đã cập nhật
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
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            return new RedirectView("/payment-result.html?status=failure&message=Không+tìm+thấy+thông+tin+thanh+toán");
        }

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

        if (payment.getPaymentMethod() == PaymentMethod.MOMO) {
            String errorCode = params.get("errorCode");
            if (errorCode != null) {
                payment.setStatus("0".equals(errorCode) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
                payment.setResponseCode(errorCode);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("Updated MOMO payment status to {} for orderId: {}",
                        payment.getStatus(), payment.getOrderId());
            }
        } else if (payment.getPaymentMethod() == PaymentMethod.VNPAY) {
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            if (vnp_ResponseCode != null) {
                payment.setStatus("00".equals(vnp_ResponseCode) ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
                payment.setResponseCode(vnp_ResponseCode);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("Updated VNPAY payment status to {} for orderId: {}",
                        payment.getStatus(), payment.getOrderId());
            }
        }
    }
}