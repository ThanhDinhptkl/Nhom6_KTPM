package com.tour.notificationservice.dto;

import lombok.Data;

@Data
public class CustomerResponseDTO {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    // các trường khác nếu cần
} 