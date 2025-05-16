package com.tour.paymentservice.services;

import com.tour.paymentservice.dto.PaymentRequestDto;
import com.tour.paymentservice.dto.PaymentResponseDto;
import com.tour.paymentservice.entities.PaymentMethod;
import com.tour.paymentservice.entities.PaymentStatus;

public interface PaymentService {
    PaymentResponseDto createPayment(PaymentRequestDto request);

    PaymentResponseDto getPaymentByOrderId(String orderId);

    /**
     * Update payment status and notify booking service if needed
     * 
     * @param orderId         Order ID
     * @param status          New payment status
     * @param transactionId   Payment gateway transaction ID
     * @param responseCode    Response code from payment gateway
     * @param responseMessage Response message
     * @param paymentMethod   The payment method that completed the payment
     *                        (optional)
     * @return Updated payment response
     */
    PaymentResponseDto updatePaymentStatus(String orderId, PaymentStatus status,
            String transactionId, String responseCode, String responseMessage, PaymentMethod paymentMethod);

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
    PaymentResponseDto updatePaymentStatus(String orderId, PaymentStatus status,
            String transactionId, String responseCode, String responseMessage);
}