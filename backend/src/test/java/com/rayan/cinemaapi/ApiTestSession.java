package com.rayan.cinemaapi;

import com.jayway.jsonpath.JsonPath;
import com.rayan.cinemaapi.entity.Role;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.repository.UserRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public record ApiTestSession(MockHttpSession session, String headerName, String token) {

    public static ApiTestSession anonymous(MockMvc mockMvc) throws Exception {
        return fetchCsrf(mockMvc, new MockHttpSession());
    }

    public static ApiTestSession signIn(MockMvc mockMvc, User user, String password) throws Exception {
        var anonymous = anonymous(mockMvc);
        var signedIn = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .formField("email", user.getEmail()).formField("password", password)
                        .with(anonymous::apply))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession(false);
        return fetchCsrf(mockMvc, signedIn);
    }

    public static ApiTestSession admin(MockMvc mockMvc, UserRepository users, PasswordEncoder encoder) throws Exception {
        String password = UUID.randomUUID().toString();
        User admin = users.save(TestDataFactory.createUser("Test", "Admin",
                UUID.randomUUID() + "@example.com", encoder.encode(password), Role.ADMIN));
        return signIn(mockMvc, admin, password);
    }

    private static ApiTestSession fetchCsrf(MockMvc mockMvc, MockHttpSession session) throws Exception {
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new ApiTestSession(session, JsonPath.read(body, "$.headerName"), JsonPath.read(body, "$.token"));
    }

    public MockHttpServletRequest apply(MockHttpServletRequest request) {
        request.setSession(session);
        request.addHeader(headerName, token);
        return request;
    }
}
