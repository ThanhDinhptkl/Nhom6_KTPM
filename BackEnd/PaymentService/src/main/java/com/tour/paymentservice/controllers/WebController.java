package com.tour.paymentservice.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller để xử lý các trang web
 */
@Controller
@Slf4j
public class WebController {

    /**
     * Chuyển hướng đến trang payment-result.html
     */
    @GetMapping("/payment-result")
    public String showPaymentResult() {
        return "redirect:/payment-result.html";
    }

    /**
     * Trang chủ
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }

    /**
     * Endpoint test hiển thị thanh toán thành công
     */
    @GetMapping("/test-success")
    public RedirectView testSuccess() {
        log.info("Testing payment success page");
        return new RedirectView(
                "/payment-result?status=success&transactionId=TEST-123&amount=100000&paymentMethod=MOMO&message=Thanh toán thành công&time="
                        + java.time.LocalDateTime.now());
    }

    /**
     * Endpoint test hiển thị thanh toán thất bại
     */
    @GetMapping("/test-failure")
    public RedirectView testFailure() {
        log.info("Testing payment failure page");
        return new RedirectView(
                "/payment-result?status=failure&transactionId=TEST-456&amount=100000&paymentMethod=VNPAY&message=Thanh toán thất bại&time="
                        + java.time.LocalDateTime.now());
    }
}