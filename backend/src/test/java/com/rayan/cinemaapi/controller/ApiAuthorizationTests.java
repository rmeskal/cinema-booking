package com.rayan.cinemaapi.controller;

import com.jayway.jsonpath.JsonPath;
import com.rayan.cinemaapi.ApiTestSession;
import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.Role;
import com.rayan.cinemaapi.repository.UserRepository;
import com.rayan.cinemaapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @ParameterizedTest
    @CsvSource({
            "POST, /api/movies", "PUT, /api/movies/1", "DELETE, /api/movies/1",
            "POST, /api/screenings", "PUT, /api/screenings/1", "DELETE, /api/screenings/1",
            "POST, /api/rooms", "PUT, /api/rooms/1", "DELETE, /api/rooms/1",
            "POST, /api/room-types", "PUT, /api/room-types/1", "DELETE, /api/room-types/1",
            "GET, /api/users", "GET, /api/users/{id}", "PUT, /api/users/{id}", "DELETE, /api/users/{id}"
    })
    void adminOperationsRequireAnAdminSession(String method, String path) throws Exception {
        String password = "test-password";
        var user = userService.createUser(TestDataFactory.createUser(), password);
        var anonymous = ApiTestSession.anonymous(mockMvc);
        var signedIn = ApiTestSession.signIn(mockMvc, user, password);

        mockMvc.perform(request(HttpMethod.valueOf(method), path, user.getId()).with(anonymous::apply))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
        mockMvc.perform(request(HttpMethod.valueOf(method), path, user.getId()).with(signedIn::apply))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/movies", "/api/screenings", "/api/rooms", "/api/room-types"})
    void catalogCanBeReadWithoutSigningIn(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void registrationWithCsrfIsPublicAndCannotChooseAnAdminRole() throws Exception {
        var anonymous = ApiTestSession.anonymous(mockMvc);
        String body = """
                {
                    "firstName": "New",
                    "lastName": "User",
                    "email": "registration@example.com",
                    "password": "test-password",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body)
                        .session(anonymous.session()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(anonymous::apply))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
        assertEquals(Role.USER, userRepository.findByEmail("registration@example.com").orElseThrow().getRole());
    }

    @Test
    void adminCanManageUsersButProfileUpdatesCannotChangeRoles() throws Exception {
        var admin = ApiTestSession.admin(mockMvc, userRepository, passwordEncoder);
        var user = userService.createUser(TestDataFactory.createUser(), "test-password");
        String path = "/api/users/" + user.getId();

        mockMvc.perform(get("/api/users").with(admin::apply)).andExpect(status().isOk());
        mockMvc.perform(get(path).with(admin::apply))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
        mockMvc.perform(put(path).with(admin::apply).contentType(MediaType.APPLICATION_JSON).content("""
                        {
                            "firstName": "Updated",
                            "lastName": "User",
                            "email": "updated@example.com",
                            "role": "ADMIN"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.role").value("USER"));
        mockMvc.perform(delete(path).with(admin::apply)).andExpect(status().isNoContent());
    }

    @Test
    void adminCanCreateMovieOnlyWithAValidCsrfToken() throws Exception {
        var admin = ApiTestSession.admin(mockMvc, userRepository, passwordEncoder);
        String body = """
                {"title": "Test Movie", "description": "A test movie", "durationInMinutes": 120}
                """;

        mockMvc.perform(post("/api/movies").session(admin.session())
                        .header(admin.headerName(), "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        String created = mockMvc.perform(post("/api/movies").with(admin::apply)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Movie"))
                .andReturn().getResponse().getContentAsString();

        Object id = JsonPath.read(created, "$.id");
        mockMvc.perform(get("/api/movies/{id}", id)).andExpect(status().isOk());
    }
}
