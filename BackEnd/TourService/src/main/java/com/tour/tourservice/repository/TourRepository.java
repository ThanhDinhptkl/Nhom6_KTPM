package com.tour.tourservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tour.tourservice.model.Tour;

public interface TourRepository extends JpaRepository<Tour, Integer>{

}
