package com.rayan.cinemaapi.controller;

import com.jayway.jsonpath.JsonPath;
import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    private User user;

    @BeforeEach
    void createExistingUser() {
        user = userService.createUser(TestDataFactory.createUser(), PASSWORD);
    }

    @Test
    void login_thenMeReturnsTheSignedInUserWithoutCredentials() throws Exception {
        MockHttpSession session = signIn();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(user.getLastName()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void me_withoutSessionReturns401WithoutRedirect() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @ParameterizedTest
    @CsvSource({EMAIL + ",wrong-password", "unknown@example.com," + PASSWORD})
    void login_withInvalidCredentialsReturnsTheSameGeneric401(String email, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        CsrfHeaders csrf = fetchCsrf(session);

        mockMvc.perform(login(email, password).session(session).header(csrf.name(), csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist("Location"));
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void login_withoutCsrfReturns403() throws Exception {
        mockMvc.perform(login(EMAIL, PASSWORD)).andExpect(status().isForbidden());
    }

    @Test
    void logout_withoutCsrfReturns403AndKeepsTheSession() throws Exception {
        MockHttpSession session = signIn();

        mockMvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
    }

    @Test
    void logout_requiresFreshCsrfAndInvalidatesTheSession() throws Exception {
        MockHttpSession beforeLogin = new MockHttpSession();
        CsrfHeaders oldCsrf = fetchCsrf(beforeLogin);
        MockHttpSession session = signIn(beforeLogin, oldCsrf);
        CsrfHeaders newCsrf = fetchCsrf(session);

        mockMvc.perform(post("/api/auth/logout").session(session).header(oldCsrf.name(), oldCsrf.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/logout").session(session).header(newCsrf.name(), newCsrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Location"));

        assertTrue(session.isInvalid());
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void defaultLogout_cannotBypassCsrfProtection() throws Exception {
        MockHttpSession session = signIn();

        mockMvc.perform(post("/logout").session(session)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
    }

    @Test
    void me_usesTheSameUserIdAfterTheEmailChanges() throws Exception {
        MockHttpSession session = signIn();
        user.setEmail("updated@example.com");
        userService.updateUser(user);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    private MockHttpSession signIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        return signIn(session, fetchCsrf(session));
    }

    private MockHttpSession signIn(MockHttpSession session, CsrfHeaders csrf) throws Exception {
        MockHttpSession signedIn = (MockHttpSession) mockMvc.perform(
                        login(EMAIL, PASSWORD).session(session).header(csrf.name(), csrf.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Location"))
                .andReturn().getRequest().getSession(false);
        assertNotNull(signedIn);
        return signedIn;
    }

    private MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login").formField("email", email).formField("password", password);
    }

    private CsrfHeaders fetchCsrf(MockHttpSession session) throws Exception {
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return new CsrfHeaders(JsonPath.read(body, "$.headerName"), JsonPath.read(body, "$.token"));
    }

    private record CsrfHeaders(String name, String token) { }
}
