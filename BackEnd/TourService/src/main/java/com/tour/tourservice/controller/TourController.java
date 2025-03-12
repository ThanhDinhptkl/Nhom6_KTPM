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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tour.tourservice.dto.TourDTO;
import com.tour.tourservice.model.Tour;
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
	
	//add list tours
	@PostMapping("/tours")
	public List<TourDTO> addTours(@RequestBody List<TourDTO> tourDTOs) {
		tourService.addList(tourDTOs);
		return tourDTOs;
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
	

	@GetMapping("/tours/location/{location}")
    public ResponseEntity<List<Tour>> getToursByLocation(@PathVariable String location) {
        List<Tour> tours = tourService.findByLocation(location);
        if (tours.isEmpty()) {
            return ResponseEntity.status(404).body(null); // Không tìm thấy
        }
        return ResponseEntity.ok(tours); // Trả về danh sách tour
    }
	
	@GetMapping("/tours/title/{title}")
	    public ResponseEntity<List<Tour>> getToursByTitle(@PathVariable String title) {
        List<Tour> tours = tourService.findByTitle(title);
        if (tours.isEmpty()) {
            return ResponseEntity.status(404).body(null); // Không tìm thấy
        }
        return ResponseEntity.ok(tours); // Trả về danh sách tour
	}
	
	@GetMapping("/tours/price")
    public ResponseEntity<List<Tour>> getToursByPriceRange(
            @RequestParam("minPrice") double minPrice,
            @RequestParam("maxPrice") double maxPrice) {
        List<Tour> tours = tourService.findByPriceRange(minPrice, maxPrice);
        if (tours.isEmpty()) {
            return ResponseEntity.status(404).body(null);
        }
        return ResponseEntity.ok(tours);
    }
	
	@DeleteMapping("/tour/{id}")
	public ResponseEntity<String> delete(@PathVariable(name = "id") int id) {
	    tourService.delete(id); // Gọi service để xóa
	    return ResponseEntity.ok("Xóa tour thành công"); // Trả về thông báo
	}
	
	@PutMapping("/tour")
	public void update(@RequestBody TourDTO tourDTO) {
		tourService.update(tourDTO);
	}
}
