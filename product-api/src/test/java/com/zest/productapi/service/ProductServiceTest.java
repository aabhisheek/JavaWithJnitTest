package com.zest.productapi.service;

import com.zest.productapi.dto.request.ItemRequest;
import com.zest.productapi.dto.request.ProductRequest;
import com.zest.productapi.dto.response.ItemResponse;
import com.zest.productapi.dto.response.PagedResponse;
import com.zest.productapi.dto.response.ProductResponse;
import com.zest.productapi.entity.Item;
import com.zest.productapi.entity.Product;
import com.zest.productapi.exception.ResourceNotFoundException;
import com.zest.productapi.repository.ItemRepository;
import com.zest.productapi.repository.ProductRepository;
import com.zest.productapi.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ItemRepository    itemRepository;

    @InjectMocks private ProductServiceImpl productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        // Set up security context so currentUsername() works
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList()));

        sampleProduct = Product.builder()
                .id(1L)
                .productName("Widget X")
                .createdBy("testuser")
                .createdOn(LocalDateTime.now())
                .build();
    }

    // ─── getAllProducts ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllProducts returns paged response")
    void getAllProducts_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> mockPage = new PageImpl<>(List.of(sampleProduct), pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(mockPage);

        PagedResponse<ProductResponse> result = productService.getAllProducts(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("Widget X");
    }

    @Test
    @DisplayName("getAllProducts returns empty page when no products exist")
    void getAllProducts_emptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(Page.empty());

        PagedResponse<ProductResponse> result = productService.getAllProducts(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ─── getProductById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getProductById returns product when found")
    void getProductById_found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo("Widget X");
    }

    @Test
    @DisplayName("getProductById throws ResourceNotFoundException when not found")
    void getProductById_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── createProduct ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct persists and returns response")
    void createProduct_success() {
        ProductRequest request = new ProductRequest();
        request.setProductName("New Product");

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p = Product.builder()
                    .id(2L)
                    .productName(p.getProductName())
                    .createdBy(p.getCreatedBy())
                    .createdOn(p.getCreatedOn())
                    .build();
            return p;
        });

        ProductResponse result = productService.createProduct(request);

        assertThat(result.getProductName()).isEqualTo("New Product");
        assertThat(result.getCreatedBy()).isEqualTo("testuser");
        verify(productRepository).save(any(Product.class));
    }

    // ─── updateProduct ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProduct modifies name and sets modifiedBy/modifiedOn")
    void updateProduct_success() {
        ProductRequest request = new ProductRequest();
        request.setProductName("Updated Product");

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, request);

        assertThat(result.getProductName()).isEqualTo("Updated Product");
        assertThat(result.getModifiedBy()).isEqualTo("testuser");
        assertThat(result.getModifiedOn()).isNotNull();
    }

    @Test
    @DisplayName("updateProduct throws when product not found")
    void updateProduct_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, new ProductRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── deleteProduct ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProduct removes the entity")
    void deleteProduct_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        doNothing().when(productRepository).delete(sampleProduct);

        assertThatCode(() -> productService.deleteProduct(1L)).doesNotThrowAnyException();
        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("deleteProduct throws when product not found")
    void deleteProduct_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getItemsByProductId ──────────────────────────────────────────────────

    @Test
    @DisplayName("getItemsByProductId returns items for existing product")
    void getItemsByProductId_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Item item = Item.builder().id(1L).product(sampleProduct).quantity(5).build();
        Page<Item> itemPage = new PageImpl<>(List.of(item), pageable, 1);

        when(productRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.findByProductId(1L, pageable)).thenReturn(itemPage);

        PagedResponse<ItemResponse> result = productService.getItemsByProductId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("getItemsByProductId throws when product not found")
    void getItemsByProductId_productNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.getItemsByProductId(99L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── addItemToProduct ─────────────────────────────────────────────────────

    @Test
    @DisplayName("addItemToProduct saves and returns item")
    void addItemToProduct_success() {
        ItemRequest request = new ItemRequest();
        request.setQuantity(10);

        Item savedItem = Item.builder().id(5L).product(sampleProduct).quantity(10).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);

        ItemResponse result = productService.addItemToProduct(1L, request);

        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getProductId()).isEqualTo(1L);
    }
}
