package com.tour.notificationservice.dto;

import lombok.Data;
import java.util.Date;

@Data
public class BookingResponseDTO {
    private int id;
    private int user_id;
    private int tour_id;
    private Date booking_date;
    private String status;
    private int number_of_people;
    private double total_price;
    private Date created_at;
} 