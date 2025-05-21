package com.tour.tourservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tour.tourservice.model.Tour;
import com.tour.tourservice.repository.TourRedisRepository;
import com.tour.tourservice.repository.TourRepository;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/redis")
public class RedisController {

    private static final Logger logger = LoggerFactory.getLogger(RedisController.class);

    @Autowired
    private TourRedisRepository tourRedisRepository;

    @Autowired
    private TourRepository tourRepository;

    @GetMapping("/tours")
    public ResponseEntity<?> getAllToursFromRedis() {
        logger.info("Getting all tours from Redis");

        Map<String, Object> response = new HashMap<>();

        try {
            List<Tour> tours = tourRedisRepository.findAll();

            if (tours != null && !tours.isEmpty()) {
                logger.info("Found {} tours in Redis cache", tours.size());
                return ResponseEntity.ok(tours);
            } else {
                // No tours in Redis, get from database
                List<Tour> dbTours = tourRepository.findAll();

                if (dbTours.isEmpty()) {
                    response.put("message", "Không có tour nào trong Redis cache và database");
                } else {
                    logger.info("No tours in Redis, but found {} tours in database", dbTours.size());
                    // Optionally update Redis with DB data
                    try {
                        tourRedisRepository.saveAll(dbTours);
                        logger.info("Updated Redis cache with tours from database");
                    } catch (Exception e) {
                        logger.warn("Không thể cập nhật Redis cache: {}", e.getMessage());
                    }
                    return ResponseEntity.ok(dbTours);
                }
            }
        } catch (RedisConnectionFailureException e) {
            logger.error("Redis connection error: {}", e.getMessage());
            response.put("error", "Redis không khả dụng hoặc bị vô hiệu hóa");

            // Redis failed, fallback to database
            List<Tour> dbTours = tourRepository.findAll();
            if (!dbTours.isEmpty()) {
                logger.info("Fetched {} tours from database as fallback", dbTours.size());
                return ResponseEntity.ok(dbTours);
            }

            response.put("message", "Không có tour nào trong database");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tour/{id}")
    public ResponseEntity<?> getTourFromRedis(@PathVariable int id) {
        logger.info("Getting tour with id: {} from Redis", id);

        Map<String, Object> response = new HashMap<>();

        try {
            if (tourRedisRepository.exists(id)) {
                Tour tour = tourRedisRepository.findById(id);
                return ResponseEntity.ok(tour);
            } else {
                // Not in Redis, try database
                Tour dbTour = tourRepository.findById(id).orElse(null);

                if (dbTour == null) {
                    response.put("message", "Tour không tồn tại trong Redis và database");
                    return ResponseEntity.status(404).body(response);
                } else {
                    // Found in DB, update Redis
                    try {
                        tourRedisRepository.save(dbTour);
                    } catch (Exception e) {
                        logger.warn("Không thể cập nhật Redis cache: {}", e.getMessage());
                    }
                    return ResponseEntity.ok(dbTour);
                }
            }
        } catch (RedisConnectionFailureException e) {
            logger.error("Redis connection error: {}", e.getMessage());
            response.put("error", "Redis không khả dụng hoặc bị vô hiệu hóa");

            // Try from database
            Tour dbTour = tourRepository.findById(id).orElse(null);
            if (dbTour != null) {
                return ResponseEntity.ok(dbTour);
            }

            response.put("message", "Tour không tồn tại trong database");
            return ResponseEntity.status(404).body(response);
        }
    }

    @DeleteMapping("/tour/{id}")
    public ResponseEntity<?> deleteTourFromCache(@PathVariable int id) {
        logger.info("Deleting tour with id: {} from Redis cache", id);

        Map<String, Object> response = new HashMap<>();

        try {
            if (!tourRedisRepository.exists(id)) {
                response.put("message", "Tour không tồn tại trong Redis cache");
                return ResponseEntity.notFound().build();
            }
            tourRedisRepository.delete(id);
            response.put("message", "Đã xóa tour khỏi Redis cache");
            return ResponseEntity.ok(response);
        } catch (RedisConnectionFailureException e) {
            logger.error("Redis connection error: {}", e.getMessage());
            response.put("error", "Redis không khả dụng hoặc bị vô hiệu hóa");
            return ResponseEntity.ok(response);
        }
    }

    @DeleteMapping("/tours")
    public ResponseEntity<?> clearRedisCache() {
        logger.info("Clearing all tours from Redis cache");

        Map<String, Object> response = new HashMap<>();

        try {
            tourRedisRepository.deleteAll();
            response.put("message", "Đã xóa tất cả tours khỏi Redis cache");
            return ResponseEntity.ok(response);
        } catch (RedisConnectionFailureException e) {
            logger.error("Redis connection error: {}", e.getMessage());
            response.put("error", "Redis không khả dụng hoặc bị vô hiệu hóa");
            return ResponseEntity.ok(response);
        }
    }
}