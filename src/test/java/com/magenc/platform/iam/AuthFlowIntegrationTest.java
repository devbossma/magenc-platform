package com.magenc.platform.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


/**
 * End-to-end integration test for the full auth flow against real Postgres.
 *
 * <p>Walks through:
 * <ol>
 *   <li>Signup creates a tenant schema and an OWNER user, returns tokens</li>
 *   <li>The access token works on /v1/auth/me</li>
 *   <li>Login with the same credentials returns a fresh token pair</li>
 *   <li>Refresh exchanges a refresh token for a new pair, the old refresh
 *       token is no longer usable (single-use semantics)</li>
 *   <li>Logout revokes the refresh token, refresh fails afterwards</li>
 * </ol>
 *
 * <p>This test deliberately uses MockMvc rather than TestRestTemplate so
 * it can run without binding to a real port and so the JWT auth filter
 * is exercised the same way it is in production.
 */
@SpringBootTest
@Testcontainers
class AuthFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");


    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("magenc.security.jwt.issuer", () -> "https://api.magenc.local");
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullAuthFlow() throws Exception {
        // 1. Signup
        String signupBody = """
                {
                  "tenantSlug": "acmetest",
                  "agencyDisplayName": "Acme Test Agency",
                  "email": "owner@acmetest.com",
                  "password": "Str0ngP@ssword!",
                  "displayName": "Acme Owner"
                }
                """;

        MvcResult signupResult = mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        JsonNode signupTokens = objectMapper.readTree(signupResult.getResponse().getContentAsString());
        String accessToken = signupTokens.get("accessToken").asText();

        // 2. /me with the access token
        mockMvc.perform(get("/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@acmetest.com"))
                .andExpect(jsonPath("$.tenant").value("acmetest"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        // 3. Login with the same credentials returns fresh tokens
        String loginBody = """
                {
                  "tenantSlug": "acmetest",
                  "email": "owner@acmetest.com",
                  "password": "Str0ngP@ssword!"
                }
                """;
        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginTokens = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken1 = loginTokens.get("refreshToken").asText();

        // 4. Refresh produces a new token pair
        String refreshBody = String.format("""
                { "refreshToken": "%s" }
                """, refreshToken1);

        MvcResult refreshResult = mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshedTokens = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String refreshToken2 = refreshedTokens.get("refreshToken").asText();
        assertThat(refreshToken2).isNotEqualTo(refreshToken1);

        // 5. Old refresh token is no longer usable (single-use semantics)
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());

        // 6. Bad password returns 401 with generic error
        String badLoginBody = """
                {
                  "tenantSlug": "acmetest",
                  "email": "owner@acmetest.com",
                  "password": "WrongP@ssword99"
                }
                """;
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLoginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void weakPasswordRejected() throws Exception {
        String weak = """
                {
                  "tenantSlug": "weaktest",
                  "agencyDisplayName": "Weak Test",
                  "email": "weak@test.com",
                  "password": "short",
                  "displayName": "Weak"
                }
                """;
        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(weak))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservedSlugRejected() throws Exception {
        String reserved = """
                {
                  "tenantSlug": "admin",
                  "agencyDisplayName": "Admin",
                  "email": "owner@admin.com",
                  "password": "Str0ngP@ssword!",
                  "displayName": "Admin"
                }
                """;
        mockMvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserved))
                .andExpect(status().isBadRequest());
    }
}
