package com.tour.tourservice.controller;

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

import com.tour.tourservice.dto.TourDTO;
import com.tour.tourservice.service.TourService;

@RestController
public class TourController {
	@Autowired
	private TourService tourService;
	
	//add new
	@PostMapping("/tour")
	public TourDTO addTour(@RequestBody TourDTO tourDTO) {
		tourService.add(tourDTO);
		return tourDTO;
	}
	
	//get all
	@GetMapping("/tours")
	public List<TourDTO> getAll() {
		return tourService.getAll();
	}
	
	@GetMapping("/tour/{id}")
	public ResponseEntity<TourDTO> get(@PathVariable(name = "id") int id) {
		return Optional.of(new ResponseEntity<TourDTO>(tourService.getOne(id), HttpStatus.OK))
				.orElse(new ResponseEntity<TourDTO>(HttpStatus.NOT_FOUND));
	}
	
	@DeleteMapping("/tour/{id}")
	public void delete(@PathVariable(name = "id") int id) {
		tourService.delete(id);
	}
	
	@PutMapping("/tour")
	public void update(@RequestBody TourDTO tourDTO) {
		tourService.update(tourDTO);
	}
}
