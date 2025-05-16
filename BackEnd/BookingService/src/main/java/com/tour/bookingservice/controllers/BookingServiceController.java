package com.tour.bookingservice.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tour.bookingservice.dto.BookingServiceDTO;
import com.tour.bookingservice.dto.PaymentRequestDTO;
import com.tour.bookingservice.dto.TourDTO;
import com.tour.bookingservice.service.BookingService;

@RestController
public class BookingServiceController {
	@Autowired
	BookingService bookingService;
	private static final Logger log = LoggerFactory.getLogger(BookingServiceController.class);

	// add new
	@PostMapping("/booking")
	public BookingServiceDTO addBooking(@RequestBody BookingServiceDTO bookingServiceDTO) {
		bookingService.add(bookingServiceDTO);
		return bookingServiceDTO;
	}

	// get all
	@GetMapping("/bookings")
	public List<BookingServiceDTO> getAll() {
		return bookingService.getAll();
	}

	@GetMapping("/booking/{id}")
	public ResponseEntity<BookingServiceDTO> get(@PathVariable(name = "id") Integer id) {
		return Optional.of(new ResponseEntity<BookingServiceDTO>(bookingService.getOne(id), HttpStatus.OK))
				.orElse(new ResponseEntity<BookingServiceDTO>(HttpStatus.NOT_FOUND));
	}

	@DeleteMapping("/booking/{id}")
	public void delete(@PathVariable(name = "id") Integer id) {
		bookingService.delete(id);
	}

	@PutMapping("/booking")
	public void update(@RequestBody BookingServiceDTO bookingServiceDTO) {
		bookingService.update(bookingServiceDTO);
	}

	@GetMapping("/tour/{id}")
	public ResponseEntity<TourDTO> getTourInfo(@PathVariable int id) {
		TourDTO tour = bookingService.getTourDetails(id);
		return tour != null ? ResponseEntity.ok(tour) : ResponseEntity.notFound().build();
	}

	/**
	 * Endpoint to initiate payment for a booking
	 */
	@PostMapping("/booking/{id}/payment")
	public ResponseEntity<Map<String, Object>> initiatePayment(
			@PathVariable("id") int bookingId,
			@RequestBody PaymentRequestDTO paymentRequest) {

		// Set booking ID from path variable
		paymentRequest.setBookingId(bookingId);

		log.info("Initiating payment for booking ID: {}, method: {}",
				bookingId, paymentRequest.getPaymentMethod());

		Map<String, Object> response = bookingService.initiatePayment(paymentRequest);

		if (response.containsKey("success") && !(Boolean) response.get("success")) {
			return ResponseEntity.badRequest().body(response);
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * Get payment status for a booking
	 */
	@GetMapping("/booking/{id}/payment")
	public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable("id") int bookingId) {
		log.info("Getting payment status for booking ID: {}", bookingId);
		Map<String, Object> response = bookingService.getPaymentStatus(bookingId);
		return ResponseEntity.ok(response);
	}

	/**
	 * Webhook to update booking status after payment completion
	 */
	@PostMapping("/booking/payment/webhook")
	public ResponseEntity<BookingServiceDTO> updateBookingAfterPayment(
			@RequestParam("bookingId") int bookingId,
			@RequestParam("paymentStatus") String paymentStatus,
			@RequestParam(value = "paymentMethod", required = false) String paymentMethod) {

		log.info("Webhook called for booking ID: {}, payment status: {}, payment method: {}",
				bookingId, paymentStatus, paymentMethod);

		BookingServiceDTO updatedBooking = bookingService.updateBookingAfterPayment(
				bookingId, paymentStatus, paymentMethod);

		if (updatedBooking == null) {
			log.error("Failed to update booking: {}, not found", bookingId);
			return ResponseEntity.notFound().build();
		}

		log.info("Successfully updated booking {} status to {}", bookingId, updatedBooking.getStatus());

		return ResponseEntity.ok(updatedBooking);
	}
}
