package com.tour.tourservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tour.tourservice.dto.TourDTO;
import com.tour.tourservice.model.Tour;
import com.tour.tourservice.repository.TourRedisRepository;
import com.tour.tourservice.repository.TourRepository;

public interface TourService {
	void add(TourDTO tourDTO);

	// add list tour
	void addList(List<TourDTO> tourDTOs);

	void update(TourDTO tourDTO);

	void delete(int id);

	List<TourDTO> getAll();

	List<Tour> findByLocation(String location);

	List<Tour> findByTitle(String title);

	TourDTO getOne(int id);

	List<Tour> findByPriceRange(double minPrice, double maxPrice);

	String uploadTourImage(MultipartFile file);
}

@Transactional
@Service
class TourServiceImpl implements TourService {

	private static final Logger logger = LoggerFactory.getLogger(TourServiceImpl.class);

	@Autowired
	TourRepository tourRepository;

	@Autowired
	TourRedisRepository tourRedisRepository;

	@Autowired
	ModelMapper modelMapper;

	@Autowired
	CloudinaryService cloudinaryService;

	@Override
	public void add(TourDTO tourDTO) {
		Tour tour = modelMapper.map(tourDTO, Tour.class);
		// Save to MySQL database
		tourRepository.save(tour);
		tourDTO.setId_tour(tour.getId_tour());

		// Save to Redis cache
		try {
			tourRedisRepository.save(tour);
		} catch (RedisConnectionFailureException e) {
			logger.warn("Redis không khả dụng. Tour đã được lưu vào database nhưng không vào cache.");
		}
	}

	@Override
	public void update(TourDTO tourDTO) {
		Tour tour = tourRepository.getById(tourDTO.getId_tour());
		if (tour != null) {
			modelMapper.typeMap(TourDTO.class, Tour.class)
					.addMappings(mapper -> mapper.skip(Tour::setCreated_at))
					.map(tourDTO, tour);

			// Update in MySQL database
			tourRepository.save(tour);

			// Update in Redis cache
			try {
				tourRedisRepository.save(tour);
			} catch (RedisConnectionFailureException e) {
				logger.warn("Redis không khả dụng. Tour đã được cập nhật trong database nhưng không trong cache.");
			}
		}
	}

	@Override
	public void delete(int id) {
		Tour tour = tourRepository.getById(id);
		if (tour != null) {
			// Delete from MySQL database
			tourRepository.delete(tour);

			// Delete from Redis cache
			try {
				tourRedisRepository.delete(id);
			} catch (RedisConnectionFailureException e) {
				logger.warn("Redis không khả dụng. Tour đã được xóa khỏi database nhưng không khỏi cache.");
			}
		}
	}

	@Override
	public List<TourDTO> getAll() {
		List<Tour> tours;
		List<TourDTO> tourDTOs = new ArrayList<>();

		// Try to get from Redis cache first
		try {
			tours = tourRedisRepository.findAll();
			if (tours != null && !tours.isEmpty()) {
				logger.info("Lấy danh sách tour từ Redis cache.");
				tours.forEach((tour) -> {
					tourDTOs.add(modelMapper.map(tour, TourDTO.class));
				});
				return tourDTOs;
			}
		} catch (RedisConnectionFailureException e) {
			logger.warn("Redis không khả dụng. Chuyển sang sử dụng database.");
		}

		// If Redis fails or cache is empty, get from database
		tours = tourRepository.findAll();
		tours.forEach((tour) -> {
			tourDTOs.add(modelMapper.map(tour, TourDTO.class));
		});

		// Try to update Redis cache
		try {
			if (tours != null && !tours.isEmpty()) {
				tourRedisRepository.saveAll(tours);
				logger.info("Cập nhật Redis cache với dữ liệu từ database.");
			}
		} catch (RedisConnectionFailureException e) {
			// Ignore Redis update if it's not available
		}

		return tourDTOs;
	}

	@Override
	public TourDTO getOne(int id) {
		Tour tour = null;

		// Try to get from Redis cache first
		try {
			if (tourRedisRepository.exists(id)) {
				tour = tourRedisRepository.findById(id);
				logger.info("Lấy tour (id: {}) từ Redis cache.", id);
			}
		} catch (RedisConnectionFailureException e) {
			logger.warn("Redis không khả dụng. Chuyển sang sử dụng database.");
		}

		// If not found in Redis, get from database
		if (tour == null) {
			tour = tourRepository.getById(id);

			// Try to update Redis cache if found in database
			if (tour != null) {
				try {
					tourRedisRepository.save(tour);
					logger.info("Cập nhật Redis cache với tour (id: {}) từ database.", id);
				} catch (RedisConnectionFailureException e) {
					// Ignore Redis update if it's not available
				}
			}
		}

		if (tour != null) {
			return modelMapper.map(tour, TourDTO.class);
		}
		return null;
	}

	@Override
	public void addList(List<TourDTO> tourDTOs) {
		List<Tour> tours = new ArrayList<>();

		for (TourDTO tourDTO : tourDTOs) {
			Tour tour = modelMapper.map(tourDTO, Tour.class);
			tourRepository.save(tour);
			tourDTO.setId_tour(tour.getId_tour());
			tours.add(tour);
		}

		// Update Redis cache with all tours
		try {
			tourRedisRepository.saveAll(tours);
			logger.info("Cập nhật Redis cache với {} tours.", tours.size());
		} catch (RedisConnectionFailureException e) {
			logger.warn("Redis không khả dụng. Tours đã được lưu vào database nhưng không vào cache.");
		}
	}

	@Override
	public List<Tour> findByLocation(String location) {
		// For this type of query, we'll go directly to the database
		// since Redis doesn't support complex queries without additional indexing
		List<Tour> tours = tourRepository.findByLocation(location);
		return tours;
	}

	@Override
	public List<Tour> findByTitle(String title) {
		// For this type of query, we'll go directly to the database
		return tourRepository.findAll().stream()
				.filter(tour -> tour.getTitle().toLowerCase().contains(title.toLowerCase()))
				.collect(Collectors.toList());
	}

	@Override
	public List<Tour> findByPriceRange(double minPrice, double maxPrice) {
		if (minPrice > maxPrice) {
			throw new IllegalArgumentException("minPrice phải nhỏ hơn hoặc bằng maxPrice");
		}
		return tourRepository.findByPriceBetween(minPrice, maxPrice).stream()
				.sorted((t1, t2) -> Double.compare(t1.getPrice(), t2.getPrice())) // Sắp xếp tăng dần
				.collect(Collectors.toList());
	}

	@Override
	public String uploadTourImage(MultipartFile file) {
		return cloudinaryService.uploadImage(file);
	}
}
