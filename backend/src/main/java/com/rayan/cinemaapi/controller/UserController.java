package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.user.UserCreateRequest;
import com.rayan.cinemaapi.dto.user.UserResponse;
import com.rayan.cinemaapi.dto.user.UserUpdateRequest;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Manage users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get all users")
    public List<UserResponse> getUsers() {
        return userService.getUsers()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID")
    public UserResponse getUser(@PathVariable Long id) {
        return toResponse(userService.getUser(id));
    }

    @PostMapping
    @Operation(summary = "Create a user")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        return toResponse(
                userService.createUser(toEntity(request), request.password())
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user")
    public UserResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        User user = toEntity(request);
        user.setId(id);

        return toResponse(userService.updateUser(user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    private User toEntity(UserCreateRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        return user;
    }

    private User toEntity(UserUpdateRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        return user;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }
}