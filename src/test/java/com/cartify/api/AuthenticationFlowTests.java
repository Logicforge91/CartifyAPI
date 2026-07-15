package com.cartify.api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "debug=false")
@ActiveProfiles("test")
class AuthenticationFlowTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void completeCustomerAuthenticationLifecycleAndAuthorization() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        String initialPassword = "SecurePass123!";
        String newPassword = "NewSecurePass456!";

        mockMvc.perform(get("/api/v1/cart/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/catalog/products"))
                .andExpect(status().isNotFound());

        JsonNode registration = json(postJson("/api/v1/auth/register", Map.of(
                "email", email,
                "password", initialPassword,
                "firstName", "Test",
                "lastName", "Customer",
                "phone", "+919000" + String.format("%06d", Math.abs(email.hashCode()) % 1_000_000))));
        String verificationToken = registration.path("data").path("developmentVerificationToken").stringValue();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", initialPassword))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", verificationToken))))
                .andExpect(status().isOk());

        JsonNode login = json(postJson("/api/v1/auth/login", Map.of(
                "email", email,
                "password", initialPassword)));
        String accessToken = login.path("data").path("accessToken").stringValue();
        String firstRefreshToken = login.path("data").path("refreshToken").stringValue();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"));

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        JsonNode refreshed = json(postJson("/api/v1/auth/refresh", Map.of("refreshToken", firstRefreshToken)));
        String rotatedRefreshToken = refreshed.path("data").path("refreshToken").stringValue();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSE"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken))))
                .andExpect(status().isUnauthorized());

        JsonNode secondLogin = json(postJson("/api/v1/auth/login", Map.of(
                "email", email,
                "password", initialPassword)));
        String logoutRefreshToken = secondLogin.path("data").path("refreshToken").stringValue();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", logoutRefreshToken))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", logoutRefreshToken))))
                .andExpect(status().isUnauthorized());

        JsonNode forgot = json(postJson("/api/v1/auth/forgot-password", Map.of("email", email)));
        String resetToken = forgot.path("data").path("developmentToken").stringValue();
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", resetToken,
                                "newPassword", newPassword))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", initialPassword))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", newPassword))))
                .andExpect(status().isOk());
    }

    @Test
    void loginAttemptsAreRateLimited() throws Exception {
        String email = "missing-" + UUID.randomUUID() + "@example.com";
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "WrongPassword123!"));
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.25");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.25");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMITED"));
    }

    @Test
    void bootstrappedAdministratorCanAuthenticateAndAccessAdminRoutes() throws Exception {
        JsonNode login = json(postJson("/api/v1/auth/login", Map.of(
                "email", "admin-test@cartify.local",
                "password", "AdminSecurePass123!")));
        String accessToken = login.path("data").path("accessToken").stringValue();

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/management/catalog/products")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    private MvcResult postJson(String path, Object body) throws Exception {
        return mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    private JsonNode json(MvcResult result) {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
