package com.zest.productapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zest.productapi.dto.request.LoginRequest;
import com.zest.productapi.dto.request.ProductRequest;
import com.zest.productapi.dto.request.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Product API Integration Tests")
class ProductIntegrationTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String adminToken;
    private static String userToken;
    private static Long   createdProductId;

    // ─── Auth Setup ───────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Register admin user")
    void registerAdmin() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("admin");
        req.setEmail("admin@test.com");
        req.setPassword("admin123");
        req.setRoles(java.util.Set.of("ROLE_ADMIN", "ROLE_USER"));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(body).path("data").path("accessToken").asText();
        Assertions.assertFalse(adminToken.isBlank());
    }

    @Test
    @Order(2)
    @DisplayName("Register regular user")
    void registerUser() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("regularuser");
        req.setEmail("user@test.com");
        req.setPassword("user123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        userToken = objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    @Test
    @Order(3)
    @DisplayName("Login returns valid token")
    void login() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    // ─── Product CRUD ─────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("POST /products creates product")
    void createProduct() throws Exception {
        ProductRequest req = new ProductRequest();
        req.setProductName("Integration Widget");

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productName").value("Integration Widget"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        createdProductId = objectMapper.readTree(body).path("data").path("id").asLong();
        Assertions.assertNotNull(createdProductId);
    }

    @Test
    @Order(5)
    @DisplayName("GET /products returns list")
    void getAllProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("GET /products/{id} returns product by id")
    void getProductById() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + createdProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdProductId));
    }

    @Test
    @Order(7)
    @DisplayName("PUT /products/{id} updates product")
    void updateProduct() throws Exception {
        ProductRequest req = new ProductRequest();
        req.setProductName("Updated Widget");

        mockMvc.perform(put("/api/v1/products/" + createdProductId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("Updated Widget"));
    }

    @Test
    @Order(8)
    @DisplayName("Regular user cannot delete product (403)")
    void userCannotDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/products/" + createdProductId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("Admin can delete product")
    void adminCanDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/products/" + createdProductId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(10)
    @DisplayName("GET /products/{id} returns 404 after delete")
    void getDeletedProduct_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + createdProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
