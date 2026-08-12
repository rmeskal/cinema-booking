package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
