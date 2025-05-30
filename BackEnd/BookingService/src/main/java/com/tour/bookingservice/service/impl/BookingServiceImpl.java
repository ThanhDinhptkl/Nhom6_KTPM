package com.tour.bookingservice.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tour.bookingservice.dto.BookingServiceDTO;
import com.tour.bookingservice.dto.PaymentRequestDTO;
import com.tour.bookingservice.dto.TourDTO;
import com.tour.bookingservice.entities.Booking;
import com.tour.bookingservice.repositories.BookingRepository;
import com.tour.bookingservice.service.BookingService;
import com.tour.bookingservice.service.PaymentServiceClient;
import com.tour.bookingservice.service.TourServiceClient;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
class BookingServiceImpl implements BookingService {
	@Autowired
	BookingRepository bookingrepository;

	@Autowired
	ModelMapper modelMapper;

	@Autowired
	private TourServiceClient tourServiceClient;

	@Autowired
	private PaymentServiceClient paymentServiceClient;

	@Autowired
	public BookingServiceImpl(TourServiceClient tourServiceClient, PaymentServiceClient paymentServiceClient) {
		this.tourServiceClient = tourServiceClient;
		this.paymentServiceClient = paymentServiceClient;
	}

	public TourDTO getTourDetails(int tourId) {
		return tourServiceClient.getTourById(tourId);
	}

	@Override
	public void add(BookingServiceDTO bookingservicedto) {
		// TODO Auto-generated method stub
		Booking booking = modelMapper.map(bookingservicedto, Booking.class);

		bookingrepository.save(booking);

		bookingservicedto.setId(booking.getId());
	}

	@Override
	public void update(BookingServiceDTO bookingservicedto) {
		// TODO Auto-generated method stub
		Booking booking = bookingrepository.getById(bookingservicedto.getId());
		if (booking != null) {
			modelMapper.map(bookingservicedto, booking);
			bookingrepository.save(booking);
		}
	}

	@Override
	public void delete(Integer id) {
		// TODO Auto-generated method stub
		Booking booking = bookingrepository.getById(id);
		if (booking != null) {
			bookingrepository.delete(booking);
		}

	}

	@Override
	public List<BookingServiceDTO> getAll() {
		// TODO Auto-generated method stub
		List<BookingServiceDTO> bookingServiceDTOs = new ArrayList<>();

		bookingrepository.findAll().forEach((booking) -> {
			bookingServiceDTOs.add(modelMapper.map(booking, BookingServiceDTO.class));
		});
		return bookingServiceDTOs;
	}

	@Override
	public BookingServiceDTO getOne(Integer id) {
		// TODO Auto-generated method stub
		Booking booking = bookingrepository.getById(id);
		if (booking != null) {
			return modelMapper.map(booking, BookingServiceDTO.class);
		}
		return null;
	}

	@Override
	public List<BookingServiceDTO> getBookingsByUserId(int userId) {
		List<BookingServiceDTO> bookingServiceDTOs = new ArrayList<>();
		bookingrepository.findByUserId(userId).forEach((booking) -> {
			bookingServiceDTOs.add(modelMapper.map(booking, BookingServiceDTO.class));
		});
		return bookingServiceDTOs;
	}

	@Override
	public Map<String, Object> initiatePayment(PaymentRequestDTO paymentRequest) {
		// Get booking details
		Booking booking = bookingrepository.getById(paymentRequest.getBookingId());
		if (booking == null) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Booking not found");
			return errorResponse;
		}

		// Verify booking is in PENDING status
		if (booking.getStatus() != Booking.Status.PENDING) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Only pending bookings can be paid");
			return errorResponse;
		}

		// Call payment service to initiate payment
		return paymentServiceClient.initiatePayment(
				booking.getId(),
				booking.getTotal_price(),
				paymentRequest.getCustomerEmail(),
				paymentRequest.getPaymentMethod());
	}

	@Override
	public Map<String, Object> getPaymentStatus(int bookingId) {
		// Get the booking first to check if it has a payment method
		Booking booking = bookingrepository.getById(bookingId);
		if (booking == null) {
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "Booking not found");
			return errorResponse;
		}

		// Get payment status using the stored payment method if available
		String paymentMethod = booking.getPaymentMethod();
		return paymentServiceClient.getPaymentStatus(bookingId, paymentMethod);
	}

	@Override
	public BookingServiceDTO updateBookingAfterPayment(int bookingId, String paymentStatus, String paymentMethod) {
		Booking booking = bookingrepository.getById(bookingId);
		if (booking == null) {
			return null;
		}

		// Update booking status based on payment status
		if ("COMPLETED".equals(paymentStatus)) {
			booking.setStatus(Booking.Status.CONFIRMED);

			// Store the payment method that completed the payment
			if (paymentMethod != null && !paymentMethod.isEmpty()) {
				booking.setPaymentMethod(paymentMethod);
			}
			// Send notification about successful payment
			try {
				String notificationUrl = String.format(
						"http://tour.phamhuuthuan.io.vn:8084/api/notifications/payment-success?userId=%d&tourId=%d&bookingId=%d",
						booking.getUser_id(), booking.getTour_id(), bookingId);

				// Create URL object
				URL url = new URL(notificationUrl);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();

				// Set up the request
				connection.setRequestMethod("POST");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(5000);

				// Get the response
				int responseCode = connection.getResponseCode();

				// Close the connection
				connection.disconnect();

				System.out.println("Payment notification sent with response: " + responseCode);
			} catch (Exception e) {
				// Log error but don't prevent booking update
				System.err.println("Failed to send payment notification: " + e.getMessage());
			}
		} else if ("FAILED".equals(paymentStatus)) {
			booking.setStatus(Booking.Status.CANCELLED);
		}

		bookingrepository.save(booking);
		return modelMapper.map(booking, BookingServiceDTO.class);
	}
}