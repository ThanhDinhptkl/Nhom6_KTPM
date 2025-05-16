package com.tour.bookingservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${payment.service.url:http://localhost:8085}")
    private String paymentServiceUrl;

    public PaymentServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Initiates a payment for a booking
     * 
     * @param bookingId     The ID of the booking
     * @param amount        The amount to be paid
     * @param email         The customer's email
     * @param paymentMethod The payment method (MOMO, VNPAY)
     * @return Payment response with payment URL and transaction details
     */
    public Map<String, Object> initiatePayment(int bookingId, double amount, String email, String paymentMethod) {
        String url = paymentServiceUrl + "/api/payments";

        Map<String, Object> request = new HashMap<>();
        request.put("orderId", String.valueOf(bookingId));
        request.put("amount", BigDecimal.valueOf(amount));
        request.put("description", "Payment for Booking #" + bookingId);
        request.put("customerEmail", email);
        request.put("paymentMethod", paymentMethod);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            return restTemplate.postForObject(url, entity, Map.class);
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();

            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to initiate payment: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Get payment status for a booking
     * 
     * @param bookingId     The booking ID
     * @param paymentMethod Optional - specific payment method to check
     * @return Payment status information
     */
    public Map<String, Object> getPaymentStatus(int bookingId, String paymentMethod) {
        String url = paymentServiceUrl + "/api/payments/" + bookingId;

        // If payment method is specified, add it as a query parameter
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            url += "?paymentMethod=" + paymentMethod;
        }

        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();

            // Return error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get payment status: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Get payment status for a booking (all payment methods)
     * 
     * @param bookingId The booking ID
     * @return Payment status information
     */
    public Map<String, Object> getPaymentStatus(int bookingId) {
        return getPaymentStatus(bookingId, null);
    }
}