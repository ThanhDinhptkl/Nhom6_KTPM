package com.tour.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.tour.notificationservice.dto.TourResponseDTO;

@FeignClient(name = "TourService",url = "http://localhost:3333")
public interface TourClient {
    @GetMapping("/tour/{id}")
    TourResponseDTO getTourById(@PathVariable("id") Integer id);
} 