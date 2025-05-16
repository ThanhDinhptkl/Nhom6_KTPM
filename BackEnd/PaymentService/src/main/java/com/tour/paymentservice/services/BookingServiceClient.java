package com.tour.paymentservice.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.tour.paymentservice.entities.PaymentMethod;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingServiceClient {

    private final RestTemplate restTemplate;

    @Value("${booking.service.url:http://localhost:5555}")
    private String bookingServiceUrl;

    public BookingServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Notify booking service about payment completion
     * 
     * @param orderId       The order ID (now directly the booking ID as String)
     * @param paymentStatus COMPLETED or FAILED
     * @param paymentMethod The payment method that was used (MOMO, VNPAY)
     * @return true if notification was successful, false otherwise
     */
    public boolean notifyPaymentCompletion(String orderId, String paymentStatus, PaymentMethod paymentMethod) {
        try {
            // Parse booking ID directly from orderId (no prefix anymore)
            int bookingId;
            try {
                bookingId = Integer.parseInt(orderId);
            } catch (NumberFormatException e) {
                log.error("Invalid order ID format: {}", orderId);
                return false;
            }

            // Build URL with query parameters
            String url = UriComponentsBuilder.fromHttpUrl(bookingServiceUrl + "/booking/payment/webhook")
                    .queryParam("bookingId", bookingId)
                    .queryParam("paymentStatus", paymentStatus)
                    .queryParam("paymentMethod", paymentMethod)
                    .toUriString();

            // Make POST request to booking service
            ResponseEntity<Object> response = restTemplate.postForEntity(url, null, Object.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to notify booking service: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Overloaded method for backward compatibility
     */
    public boolean notifyPaymentCompletion(String orderId, String paymentStatus) {
        return notifyPaymentCompletion(orderId, paymentStatus, null);
    }
}