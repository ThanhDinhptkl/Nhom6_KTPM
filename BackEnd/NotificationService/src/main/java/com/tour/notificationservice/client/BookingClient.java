package com.tour.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.tour.notificationservice.dto.BookingResponseDTO;

@FeignClient(name = "BookingService",url = "http://localhost:5555")
public interface BookingClient {
    @GetMapping("/booking/{id}")
    BookingResponseDTO getBookingById(@PathVariable("id") Integer id);
} 