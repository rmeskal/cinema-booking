package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.dto.user.UserCreateRequest;
import com.rayan.cinemaapi.dto.user.UserUpdateRequest;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void getUsers_returnsUsers() throws Exception {
        User user = TestDataFactory.createUser();
        user.setId(1L);

        when(userService.getUsers())
                .thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Test"))
                .andExpect(jsonPath("$[0].lastName").value("User"))
                .andExpect(jsonPath("$[0].email").value("test@example.com"))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].bookings").doesNotExist());
    }

    @Test
    void getUsers_returnsEmptyListWhenNoUsersExist() throws Exception {
        when(userService.getUsers())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getUser_returnsUserWhenFound() throws Exception {
        User user = TestDataFactory.createUser();
        user.setId(1L);

        when(userService.getUser(1L))
                .thenReturn(user);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.bookings").doesNotExist());
    }

    @Test
    void getUser_returnsNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.getUser(999L))
                .thenThrow(new EntityNotFoundException("User", 999L));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 999 was not found"));
    }

    @Test
    void createUser_createsUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Test",
                "User",
                "test@example.com",
                "password123"
        );

        User createdUser = TestDataFactory.createUser();
        createdUser.setId(1L);

        when(userService.createUser(any(User.class), eq("password123")))
                .thenReturn(createdUser);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.bookings").doesNotExist());

        verify(userService)
                .createUser(any(User.class), eq("password123"));
    }

    @Test
    void createUser_returnsBadRequestWhenFirstNameIsBlank() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "",
                "User",
                "test@example.com",
                "password123"
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .createUser(any(User.class), anyString());
    }

    @Test
    void createUser_returnsBadRequestWhenEmailIsInvalid() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Test",
                "User",
                "invalid-email",
                "password123"
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .createUser(any(User.class), anyString());
    }

    @Test
    void createUser_returnsBadRequestWhenPasswordIsTooShort() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Test",
                "User",
                "test@example.com",
                "short"
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .createUser(any(User.class), anyString());
    }

    @Test
    void updateUser_updatesUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest(
                "Updated",
                "User",
                "updated@example.com"
        );

        User updatedUser = TestDataFactory.createUser(
                "Updated",
                "User",
                "updated@example.com",
                "hashed-password",
                com.rayan.cinemaapi.entity.Role.USER
        );
        updatedUser.setId(1L);

        when(userService.updateUser(any(User.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.bookings").doesNotExist());

        verify(userService)
                .updateUser(any(User.class));
    }

    @Test
    void updateUser_returnsNotFoundWhenUserDoesNotExist() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest(
                "Updated",
                "User",
                "updated@example.com"
        );

        when(userService.updateUser(any(User.class)))
                .thenThrow(new EntityNotFoundException("User", 999L));

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 999 was not found"));
    }

    @Test
    void updateUser_returnsBadRequestWhenFirstNameIsBlank() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest(
                "",
                "User",
                "updated@example.com"
        );

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .updateUser(any(User.class));
    }

    @Test
    void updateUser_returnsBadRequestWhenEmailIsInvalid() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest(
                "Updated",
                "User",
                "invalid-email"
        );

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never())
                .updateUser(any(User.class));
    }

    @Test
    void deleteUser_deletesUser() throws Exception {
        doNothing().when(userService)
                .deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService)
                .deleteUser(1L);
    }

    @Test
    void deleteUser_returnsNotFoundWhenUserDoesNotExist() throws Exception {
        doThrow(new EntityNotFoundException("User", 999L))
                .when(userService)
                .deleteUser(999L);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("User with id 999 was not found"));

        verify(userService)
                .deleteUser(999L);
    }
}