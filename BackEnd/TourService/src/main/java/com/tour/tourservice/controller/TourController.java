package com.tour.tourservice.controller;

import java.util.List;
import java.util.Optional;
import java.util.Date;
import java.util.Calendar;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tour.tourservice.dto.TourDTO;
import com.tour.tourservice.model.Tour;
import com.tour.tourservice.service.TourService;

@RestController
@CrossOrigin(origins = "*")
public class TourController {

	private static final Logger logger = LoggerFactory.getLogger(TourController.class);

	@Autowired
	private TourService tourService;

	// add new
	@PostMapping("/tour")
	public ResponseEntity<?> addTour(@RequestBody TourDTO tourDTO) {
		try {
			logger.info("Adding new tour: {}", tourDTO.getTitle());
			tourService.add(tourDTO);
			return ResponseEntity.ok(tourDTO);
		} catch (Exception e) {
			logger.error("Error adding tour: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to add tour: " + e.getMessage());
		}
	}

	// add list tours
	@PostMapping("/tours")
	public ResponseEntity<?> addTours(@RequestBody List<TourDTO> tourDTOs) {
		try {
			logger.info("Adding {} tours", tourDTOs.size());
			tourService.addList(tourDTOs);
			return ResponseEntity.ok(tourDTOs);
		} catch (Exception e) {
			logger.error("Error adding tours: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to add tours: " + e.getMessage());
		}
	}

	// get all
	@GetMapping("/tours")
	public ResponseEntity<List<TourDTO>> getAll() {
		try {
			logger.info("Getting all tours");
			List<TourDTO> tours = tourService.getAll();
			return ResponseEntity.ok(tours);
		} catch (Exception e) {
			logger.error("Error getting all tours: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/tour/{id}")
	public ResponseEntity<TourDTO> get(@PathVariable(name = "id") int id) {
		try {
			logger.info("Getting tour with id: {}", id);
			TourDTO tour = tourService.getOne(id);
			if (tour != null) {
				return ResponseEntity.ok(tour);
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception e) {
			logger.error("Error getting tour with id {}: {}", id, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/tours/location/{location}")
	public ResponseEntity<List<Tour>> getToursByLocation(@PathVariable String location) {
		try {
			logger.info("Getting tours by location: {}", location);
			List<Tour> tours = tourService.findByLocation(location);
			if (tours.isEmpty()) {
				return ResponseEntity.status(404).body(null); // Không tìm thấy
			}
			return ResponseEntity.ok(tours); // Trả về danh sách tour
		} catch (Exception e) {
			logger.error("Error getting tours by location {}: {}", location, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/tours/title/{title}")
	public ResponseEntity<List<Tour>> getToursByTitle(@PathVariable String title) {
		try {
			logger.info("Getting tours by title: {}", title);
			List<Tour> tours = tourService.findByTitle(title);
			if (tours.isEmpty()) {
				return ResponseEntity.status(404).body(null); // Không tìm thấy
			}
			return ResponseEntity.ok(tours); // Trả về danh sách tour
		} catch (Exception e) {
			logger.error("Error getting tours by title {}: {}", title, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/tours/price")
	public ResponseEntity<List<Tour>> getToursByPriceRange(
			@RequestParam("minPrice") double minPrice,
			@RequestParam("maxPrice") double maxPrice) {
		try {
			logger.info("Getting tours by price range: {} - {}", minPrice, maxPrice);
			List<Tour> tours = tourService.findByPriceRange(minPrice, maxPrice);
			if (tours.isEmpty()) {
				return ResponseEntity.status(404).body(null);
			}
			return ResponseEntity.ok(tours);
		} catch (Exception e) {
			logger.error("Error getting tours by price range {} - {}: {}", minPrice, maxPrice, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DeleteMapping("/tour/{id}")
	public ResponseEntity<String> delete(@PathVariable(name = "id") int id) {
		try {
			logger.info("Deleting tour with id: {}", id);
			tourService.delete(id); // Gọi service để xóa
			return ResponseEntity.ok("Xóa tour thành công"); // Trả về thông báo
		} catch (Exception e) {
			logger.error("Error deleting tour with id {}: {}", id, e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to delete tour: " + e.getMessage());
		}
	}

	@PutMapping("/tour")
	public ResponseEntity<?> update(@RequestBody TourDTO tourDTO) {
		try {
			logger.info("Updating tour with id: {}", tourDTO.getId_tour());
			tourService.update(tourDTO);
			return ResponseEntity.ok(tourDTO);
		} catch (Exception e) {
			logger.error("Error updating tour with id {}: {}", tourDTO.getId_tour(), e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to update tour: " + e.getMessage());
		}
	}

	@PostMapping("/tour/upload-image")
	public ResponseEntity<String> uploadTourImage(@RequestParam("file") MultipartFile file) {
		try {
			logger.info("Uploading tour image: {}", file.getOriginalFilename());
			String imageUrl = tourService.uploadTourImage(file);
			return ResponseEntity.ok(imageUrl);
		} catch (Exception e) {
			logger.error("Error uploading tour image: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to upload image: " + e.getMessage());
		}
	}

	@PostMapping("/tour/with-image")
	public ResponseEntity<?> addTourWithImage(
			@RequestParam("tour") String tourJson,
			@RequestParam("file") MultipartFile file) {
		try {
			logger.info("Adding new tour with image: {}", file.getOriginalFilename());

			// Chuyển tourJson (dạng String) thành TourDTO
			ObjectMapper objectMapper = new ObjectMapper();
			TourDTO tourDTO = objectMapper.readValue(tourJson, TourDTO.class);

			// Upload ảnh lên Cloudinary
			String imageUrl = tourService.uploadTourImage(file);
			tourDTO.setImage(imageUrl); // gán link ảnh vào tour

			// Lưu tour
			tourService.add(tourDTO);
			return ResponseEntity.ok(tourDTO);

		} catch (Exception e) {
			logger.error("Error adding tour with image: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Failed to add tour with image: " + e.getMessage());
		}
	}

	// Tạo API endpoint mới để tạo dữ liệu mẫu
	@PostMapping("/tours/sample")
	public ResponseEntity<?> createSampleTours() {
		try {
			logger.info("Creating sample tour data");

			// Tạo danh sách tour mẫu
			List<TourDTO> tourDTOs = new ArrayList<>();

			TourDTO tour1 = new TourDTO();
			tour1.setTitle("Tour Hạ Long");
			tour1.setDescription("Khám phá Vịnh Hạ Long với các hoạt động thú vị");
			tour1.setLocation("Hạ Long, Quảng Ninh");
			tour1.setDuration(3);
			tour1.setPrice(2500000);
			tour1.setMax_participants(20);
			tour1.setStart_date(new Date());

			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 3);
			tour1.setEnd_date(calendar.getTime());
			tour1.setCreated_at(new Date());
			tour1.setImage("https://example.com/halong.jpg");

			TourDTO tour2 = new TourDTO();
			tour2.setTitle("Tour Đà Nẵng - Hội An");
			tour2.setDescription("Khám phá Đà Nẵng và phố cổ Hội An");
			tour2.setLocation("Đà Nẵng, Quảng Nam");
			tour2.setDuration(4);
			tour2.setPrice(3000000);
			tour2.setMax_participants(25);
			tour2.setStart_date(new Date());

			calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 4);
			tour2.setEnd_date(calendar.getTime());
			tour2.setCreated_at(new Date());
			tour2.setImage("https://example.com/danang.jpg");

			TourDTO tour3 = new TourDTO();
			tour3.setTitle("Tour Phú Quốc");
			tour3.setDescription("Nghỉ dưỡng tại đảo ngọc Phú Quốc");
			tour3.setLocation("Phú Quốc, Kiên Giang");
			tour3.setDuration(5);
			tour3.setPrice(5000000);
			tour3.setMax_participants(30);
			tour3.setStart_date(new Date());

			calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, 5);
			tour3.setEnd_date(calendar.getTime());
			tour3.setCreated_at(new Date());
			tour3.setImage("https://example.com/phuquoc.jpg");

			tourDTOs.add(tour1);
			tourDTOs.add(tour2);
			tourDTOs.add(tour3);

			tourService.addList(tourDTOs);

			return ResponseEntity.ok("Đã tạo " + tourDTOs.size() + " tour mẫu thành công");
		} catch (Exception e) {
			logger.error("Error creating sample tours: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Lỗi khi tạo tour mẫu: " + e.getMessage());
		}
	}

}
