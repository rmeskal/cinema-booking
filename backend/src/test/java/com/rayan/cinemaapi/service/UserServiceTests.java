package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUser_throwsExceptionWhenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> userService.getUser(1L)
        );
    }

    @Test
    void updateUser_updatesFields() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");
        existingUser.setFirstName("Old");
        existingUser.setLastName("Name");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail("new@example.com");
        updatedUser.setFirstName("New");
        updatedUser.setLastName("Name");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existingUser));

        User result = userService.updateUser(updatedUser);

        assertEquals("new@example.com", result.getEmail());
        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
    }
}