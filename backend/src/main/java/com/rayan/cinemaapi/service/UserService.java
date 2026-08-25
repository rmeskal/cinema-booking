package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.rayan.cinemaapi.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("User", id)
        );
    }

    public User createUser(User user, String password) {
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.USER);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(User user) {
        User existingUser = getUser(user.getId());

        existingUser.setEmail(user.getEmail());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());

        return existingUser;
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
