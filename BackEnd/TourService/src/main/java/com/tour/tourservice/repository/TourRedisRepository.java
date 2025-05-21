package com.tour.tourservice.repository;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.data.redis.serializer.SerializationException;

import com.tour.tourservice.model.Tour;

@Repository
public class TourRedisRepository {

    private static final Logger logger = LoggerFactory.getLogger(TourRedisRepository.class);
    private static final String HASH_KEY = "TOUR";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Cache expiration time (30 minutes)
    private final long CACHE_TTL = 30;

    // Create/Save tour to Redis
    public void save(Tour tour) {
        try {
            redisTemplate.opsForHash().put(HASH_KEY, tour.getId_tour(), tour);
            redisTemplate.expire(HASH_KEY + ":" + tour.getId_tour(), CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("Error saving tour to Redis: {}", e.getMessage());
        }
    }

    // Create/Save multiple tours to Redis
    public void saveAll(List<Tour> tours) {
        try {
            Map<Integer, Tour> tourMap = new java.util.HashMap<>();
            for (Tour tour : tours) {
                tourMap.put(tour.getId_tour(), tour);
            }
            redisTemplate.opsForHash().putAll(HASH_KEY, tourMap);
        } catch (Exception e) {
            logger.error("Error saving tours to Redis: {}", e.getMessage());
        }
    }

    // Find tour by id
    public Tour findById(int id) {
        try {
            Object obj = redisTemplate.opsForHash().get(HASH_KEY, id);
            if (obj == null) {
                return null;
            }
            return convertToTour(obj);
        } catch (Exception e) {
            logger.error("Error finding tour in Redis: {}", e.getMessage());
            return null;
        }
    }

    // Find all tours
    public List<Tour> findAll() {
        try {
            List<Object> objects = redisTemplate.opsForHash().values(HASH_KEY);
            List<Tour> tours = new ArrayList<>();

            if (objects != null && !objects.isEmpty()) {
                for (Object obj : objects) {
                    Tour tour = convertToTour(obj);
                    if (tour != null) {
                        tours.add(tour);
                    }
                }
            }

            return tours;
        } catch (Exception e) {
            logger.error("Error finding all tours in Redis: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper method to safely convert Object to Tour
    private Tour convertToTour(Object obj) {
        try {
            if (obj instanceof Tour) {
                // Try direct cast first (rarely works after reload)
                return (Tour) obj;
            } else {
                // If we have the object serialized in Redis but cannot cast,
                // return null and let the service fetch from database
                logger.warn("Cannot convert Redis object to Tour, will use database instead");
                return null;
            }
        } catch (ClassCastException e) {
            logger.error("ClassCastException when converting Redis object: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error converting Redis object: {}", e.getMessage());
            return null;
        }
    }

    // Delete tour
    public void delete(int id) {
        try {
            redisTemplate.opsForHash().delete(HASH_KEY, id);
        } catch (Exception e) {
            logger.error("Error deleting tour from Redis: {}", e.getMessage());
        }
    }

    // Clear all tours
    public void deleteAll() {
        try {
            redisTemplate.delete(HASH_KEY);
        } catch (Exception e) {
            logger.error("Error clearing Redis cache: {}", e.getMessage());
        }
    }

    // Check if tour exists
    public boolean exists(int id) {
        try {
            return redisTemplate.opsForHash().hasKey(HASH_KEY, id);
        } catch (Exception e) {
            logger.error("Error checking if tour exists in Redis: {}", e.getMessage());
            return false;
        }
    }
}