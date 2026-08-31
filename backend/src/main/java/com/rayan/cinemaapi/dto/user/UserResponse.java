package com.rayan.cinemaapi.dto.user;

import com.rayan.cinemaapi.entity.Role;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role
) {
}