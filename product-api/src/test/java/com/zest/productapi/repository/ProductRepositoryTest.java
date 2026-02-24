package com.zest.productapi.repository;

import com.zest.productapi.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProductRepository Integration Tests")
class ProductRepositoryTest {

    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        productRepository.save(product("Widget Alpha"));
        productRepository.save(product("Gadget Beta"));
        productRepository.save(product("Widget Gamma"));
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns all persisted products with pagination")
    void findAll_returnsAll() {
        Page<Product> page = productRepository.findAll(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Product::getProductName)
                .containsExactlyInAnyOrder("Widget Alpha", "Gadget Beta", "Widget Gamma");
    }

    @Test
    @DisplayName("findAll respects page size")
    void findAll_pageSizeRespected() {
        Page<Product> page = productRepository.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isLast()).isFalse();
    }

    // ─── findById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns product when present")
    void findById_found() {
        Product saved = productRepository.save(product("Find Me"));

        Optional<Product> result = productRepository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getProductName()).isEqualTo("Find Me");
    }

    @Test
    @DisplayName("findById returns empty when absent")
    void findById_notFound() {
        Optional<Product> result = productRepository.findById(999L);

        assertThat(result).isEmpty();
    }

    // ─── findByProductNameContainingIgnoreCase ────────────────────────────────

    @Test
    @DisplayName("name search returns matching products (case-insensitive)")
    void searchByName_caseInsensitive() {
        Page<Product> result = productRepository
                .findByProductNameContainingIgnoreCase("widget", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Product::getProductName)
                .containsExactlyInAnyOrder("Widget Alpha", "Widget Gamma");
    }

    @Test
    @DisplayName("name search returns empty page when no match")
    void searchByName_noMatch() {
        Page<Product> result = productRepository
                .findByProductNameContainingIgnoreCase("zzz", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("name search handles partial match anywhere in name")
    void searchByName_partialMatch() {
        Page<Product> result = productRepository
                .findByProductNameContainingIgnoreCase("eta", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("Gadget Beta");
    }

    // ─── save / delete ────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete removes the product")
    void delete_removesProduct() {
        Product saved = productRepository.save(product("Delete Me"));
        Long id = saved.getId();

        productRepository.delete(saved);

        assertThat(productRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("existsById returns true for persisted product")
    void existsById_true() {
        Product saved = productRepository.save(product("Exists Test"));

        assertThat(productRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    @DisplayName("existsById returns false for unknown id")
    void existsById_false() {
        assertThat(productRepository.existsById(Long.MAX_VALUE)).isFalse();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Product product(String name) {
        return Product.builder()
                .productName(name)
                .createdBy("test-user")
                .createdOn(LocalDateTime.now())
                .build();
    }
}
