package com.airline.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.airline.user.entity.Passenger;
import java.util.Optional;

/**
 * Repository for Passenger entity.
 */
public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    /**
     * Find passenger profile by user ID.
     */
    Optional<Passenger> findByUserId(Long userId);
}
