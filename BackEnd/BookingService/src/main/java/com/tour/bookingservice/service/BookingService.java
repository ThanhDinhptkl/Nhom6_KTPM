package com.tour.bookingservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tour.bookingservice.dto.BookingServiceDTO;
import com.tour.bookingservice.dto.PaymentRequestDTO;
import com.tour.bookingservice.dto.TourDTO;
import com.tour.bookingservice.entities.Booking;
import com.tour.bookingservice.repositories.BookingRepository;

public interface BookingService {
	void add(BookingServiceDTO bookingservicedto);

	void update(BookingServiceDTO bookingservicedto);

	void delete(Integer id);

	List<BookingServiceDTO> getAll();

	BookingServiceDTO getOne(Integer id);

	TourDTO getTourDetails(int tourId);

	/**
	 * Get all bookings for a specific user
	 * 
	 * @param userId User ID
	 * @return List of bookings for the user
	 */
	List<BookingServiceDTO> getBookingsByUserId(int userId);

	/**
	 * Initiate payment for a booking
	 * 
	 * @param paymentRequest Payment request details
	 * @return Payment response with payment URL and transaction details
	 */
	Map<String, Object> initiatePayment(PaymentRequestDTO paymentRequest);

	/**
	 * Get the payment status for a booking
	 * 
	 * @param bookingId Booking ID
	 * @return Payment status details
	 */
	Map<String, Object> getPaymentStatus(int bookingId);

	/**
	 * Update booking status based on payment result
	 * 
	 * @param bookingId     Booking ID
	 * @param paymentStatus Payment status (COMPLETED, FAILED)
	 * @param paymentMethod Payment method
	 * @return Updated booking details
	 */
	BookingServiceDTO updateBookingAfterPayment(int bookingId, String paymentStatus, String paymentMethod);
}
