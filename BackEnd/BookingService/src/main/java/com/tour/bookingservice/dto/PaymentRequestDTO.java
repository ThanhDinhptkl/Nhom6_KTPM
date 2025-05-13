package com.tour.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDTO {
    private int bookingId;
    private String customerEmail;
    private String paymentMethod; // "MOMO" or "VNPAY"
}