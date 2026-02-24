package com.zest.productapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zest.productapi.dto.request.ProductRequest;
import com.zest.productapi.dto.response.ApiResponse;
import com.zest.productapi.dto.response.PagedResponse;
import com.zest.productapi.dto.response.ProductResponse;
import com.zest.productapi.security.JwtTokenProvider;
import com.zest.productapi.security.UserDetailsServiceImpl;
import com.zest.productapi.service.ProductService;
import com.zest.productapi.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
@DisplayName("ProductController Web Layer Tests")
class ProductControllerTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;

    @MockBean private ProductService         productService;
    @MockBean private JwtTokenProvider       jwtTokenProvider;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private TokenBlacklistService  tokenBlacklistService;

    private ProductResponse sampleResponse() {
        return ProductResponse.builder()
                .id(1L)
                .productName("Widget X")
                .createdBy("admin")
                .createdOn(LocalDateTime.now())
                .build();
    }

    // ─── GET /api/v1/products ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/products returns 200 with paged list")
    void getAllProducts_returns200() throws Exception {
        PagedResponse<ProductResponse> paged = PagedResponse.<ProductResponse>builder()
                .content(List.of(sampleResponse()))
                .page(0).size(10).totalElements(1).totalPages(1).last(true)
                .build();

        when(productService.getAllProducts(any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].productName").value("Widget X"));
    }

    // ─── GET /api/v1/products/{id} ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/products/1 returns 200 with product")
    void getProductById_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productName").value("Widget X"));
    }

    // ─── POST /api/v1/products ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/products returns 201 on valid request")
    void createProduct_returns201() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setProductName("New Widget");

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/products returns 400 when productName is blank")
    void createProduct_returns400_whenNameBlank() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setProductName("");

        mockMvc.perform(post("/api/v1/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── PUT /api/v1/products/{id} ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("PUT /api/v1/products/1 returns 200 on success")
    void updateProduct_returns200() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setProductName("Updated Widget");

        ProductResponse updated = sampleResponse();
        updated.setProductName("Updated Widget");

        when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/products/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("Updated Widget"));
    }

    // ─── DELETE /api/v1/products/{id} ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/v1/products/1 returns 204 for ADMIN")
    void deleteProduct_admin_returns204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/v1/products/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/v1/products/1 returns 403 for USER")
    void deleteProduct_user_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ─── Unauthenticated ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/products returns 401 when not authenticated")
    void getAllProducts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }
}
