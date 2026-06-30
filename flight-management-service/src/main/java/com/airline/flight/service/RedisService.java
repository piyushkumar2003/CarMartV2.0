package com.airline.flight.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Lock seat when passenger starts booking
    // TTL = 10 minutes
    // Key format: seat:{flightNumber}:{seatNumber}
    public boolean lockSeat(String flightNumber, String seatNumber) {
        String key = "seat:" + flightNumber + ":" + seatNumber;
        // setIfAbsent is atomic (SETNX), returns true if key was set, false if already
        // exists
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", 10, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(success);
    }

    public boolean isSeatLocked(String flightNumber, String seatNumber) {
        String key = "seat:" + flightNumber + ":" + seatNumber;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void releaseSeat(String flightNumber, String seatNumber) {
        String key = "seat:" + flightNumber + ":" + seatNumber;
        redisTemplate.delete(key);
    }

    // Cache search results
    // Usage: source + destination + date
    // TTL = 5 minutes
    public void cacheSearchResults(String source, String destination, String date, Object results) {
        String key = "search:" + source + ":" + destination + ":" + date;
        redisTemplate.opsForValue().set(key, results, 5, TimeUnit.MINUTES);
    }

    public Object getCachedSearchResults(String source, String destination, String date) {
        String key = "search:" + source + ":" + destination + ":" + date;
        return redisTemplate.opsForValue().get(key);
    }

    public void clearSearchCache(String source, String destination, String date) {
        String key = "search:" + source + ":" + destination + ":" + date;
        redisTemplate.delete(key);
    }
}
