package com.airline.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.airline.user.entity.Passenger;
import com.airline.user.entity.FrequentTraveler;
import com.airline.user.repository.PassengerRepository;
import com.airline.user.repository.FrequentTravelerRepository;
import java.util.List;

/**
 * Service for managing passenger profiles and frequent travelers.
 */
@Service
public class PassengerService {

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private FrequentTravelerRepository frequentTravelerRepository;

    // ===========================================
    // PASSENGER PROFILE
    // ===========================================

    /**
     * Get passenger profile for a user.
     * Creates a new empty profile if none exists.
     */
    public Passenger getOrCreatePassenger(Long userId) {
        return passengerRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Passenger newPassenger = new Passenger(userId);
                    return passengerRepository.save(newPassenger);
                });
    }

    /**
     * Get passenger by user ID (returns null if not found).
     */
    public Passenger getPassengerByUserId(Long userId) {
        return passengerRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Update passenger profile.
     */
    public Passenger updateProfile(Passenger passenger) {
        return passengerRepository.save(passenger);
    }

    // ===========================================
    // FREQUENT TRAVELERS
    // ===========================================

    /**
     * Get all frequent travelers for a user.
     */
    public List<FrequentTraveler> getFrequentTravelers(Long userId) {
        return frequentTravelerRepository.findByUserId(userId);
    }

    /**
     * Add a new frequent traveler.
     */
    public FrequentTraveler addFrequentTraveler(FrequentTraveler traveler) {
        return frequentTravelerRepository.save(traveler);
    }

    /**
     * Delete a frequent traveler.
     * Only allows deletion if the traveler belongs to the user.
     */
    public boolean deleteFrequentTraveler(Long travelerId, Long userId) {
        FrequentTraveler traveler = frequentTravelerRepository.findById(travelerId).orElse(null);

        if (traveler != null && traveler.getUserId().equals(userId)) {
            frequentTravelerRepository.delete(traveler);
            return true;
        }
        return false;
    }

    /**
     * Get a specific frequent traveler by ID.
     */
    public FrequentTraveler getFrequentTravelerById(Long id) {
        return frequentTravelerRepository.findById(id).orElse(null);
    }
}
