package com.airline.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.airline.user.entity.FrequentTraveler;
import java.util.List;

/**
 * Repository for FrequentTraveler entity.
 */
public interface FrequentTravelerRepository extends JpaRepository<FrequentTraveler, Long> {

    /**
     * Find all frequent travelers for a user.
     */
    List<FrequentTraveler> findByUserId(Long userId);
}
