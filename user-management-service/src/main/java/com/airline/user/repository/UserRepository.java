package com.airline.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.airline.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
