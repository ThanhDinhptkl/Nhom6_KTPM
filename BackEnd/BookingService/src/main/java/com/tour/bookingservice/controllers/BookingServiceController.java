package com.tour.bookingservice.controllers;

import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import com.tour.bookingservice.dto.BookingServiceDTO;
import com.tour.bookingservice.service.BookingService;

@RestController
public class BookingServiceController {
	@Autowired
	BookingService bookingService;
	
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
}
