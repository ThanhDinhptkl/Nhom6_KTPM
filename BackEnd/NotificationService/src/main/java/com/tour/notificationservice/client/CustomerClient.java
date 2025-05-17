package com.tour.notificationservice.client;

import com.tour.notificationservice.dto.CustomerResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", url = "http://localhost:8081")
public interface CustomerClient {
    @GetMapping("/customer/{id}")
    CustomerResponseDTO getCustomerById(@PathVariable("id") Integer id);
} 