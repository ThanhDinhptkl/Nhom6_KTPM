package com.tour.notificationservice.dto;

import lombok.Data;
import java.util.Date;

@Data
public class TourResponseDTO {
    private int id_tour;
    private String title;
    private String description;
    private String location;
    private int duration;
    private double price;
    private int max_participants;
    private Date start_date;
    private Date end_date;
    private Date created_at;
    private String image;
} 