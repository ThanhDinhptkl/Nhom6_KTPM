package com.tour.bookingservice.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tour.bookingservice.entities.Booking;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    /**
     * Find all bookings by user ID
     * 
     * @param userId User ID
     * @return List of bookings for the specified user
     */
    @Query("SELECT b FROM Booking b WHERE b.user_id = :userId")
    List<Booking> findByUserId(@Param("userId") int userId);

}
