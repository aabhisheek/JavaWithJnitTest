package com.zest.productapi.service;

import com.zest.productapi.dto.request.ItemRequest;
import com.zest.productapi.dto.request.ProductRequest;
import com.zest.productapi.dto.response.ItemResponse;
import com.zest.productapi.dto.response.PagedResponse;
import com.zest.productapi.dto.response.ProductResponse;
import com.zest.productapi.entity.Item;
import com.zest.productapi.entity.Product;
import com.zest.productapi.exception.ResourceNotFoundException;
import com.zest.productapi.mapper.ItemMapper;
import com.zest.productapi.mapper.ProductMapper;
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
    @Mock private ProductMapper     productMapper;
    @Mock private ItemMapper        itemMapper;

    @InjectMocks private ProductServiceImpl productService;

    private Product sampleProduct;
    private ProductResponse sampleResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "testuser", null, Collections.emptyList()));

        sampleProduct = Product.builder()
                .id(1L)
                .productName("Widget X")
                .createdBy("testuser")
                .createdOn(LocalDateTime.now())
                .build();

        sampleResponse = ProductResponse.builder()
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
        when(productMapper.toResponse(sampleProduct)).thenReturn(sampleResponse);

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
        when(productMapper.toResponse(sampleProduct)).thenReturn(sampleResponse);

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
    @DisplayName("createProduct persists and returns mapped response")
    void createProduct_success() {
        ProductRequest request = new ProductRequest();
        request.setProductName("New Product");

        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productMapper.toResponse(sampleProduct)).thenReturn(sampleResponse);

        ProductResponse result = productService.createProduct(request);

        assertThat(result.getProductName()).isEqualTo("Widget X");
        verify(productRepository).save(any(Product.class));
        verify(productMapper).toResponse(sampleProduct);
    }

    // ─── updateProduct ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProduct modifies fields and returns mapped response")
    void updateProduct_success() {
        ProductRequest request = new ProductRequest();
        request.setProductName("Updated Product");

        ProductResponse updatedResponse = ProductResponse.builder()
                .id(1L).productName("Updated Product")
                .createdBy("testuser").createdOn(LocalDateTime.now())
                .modifiedBy("testuser").modifiedOn(LocalDateTime.now())
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productMapper.toResponse(sampleProduct)).thenReturn(updatedResponse);

        ProductResponse result = productService.updateProduct(1L, request);

        assertThat(result.getModifiedBy()).isEqualTo("testuser");
        assertThat(result.getModifiedOn()).isNotNull();
        verify(productRepository).save(sampleProduct);
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
    @DisplayName("getItemsByProductId returns mapped items for existing product")
    void getItemsByProductId_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Item item = Item.builder().id(1L).product(sampleProduct).quantity(5).build();
        ItemResponse itemResponse = ItemResponse.builder().id(1L).productId(1L).quantity(5).build();
        Page<Item> itemPage = new PageImpl<>(List.of(item), pageable, 1);

        when(productRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.findByProductId(1L, pageable)).thenReturn(itemPage);
        when(itemMapper.toResponse(item)).thenReturn(itemResponse);

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
    @DisplayName("addItemToProduct saves and returns mapped item")
    void addItemToProduct_success() {
        ItemRequest request = new ItemRequest();
        request.setQuantity(10);

        Item savedItem = Item.builder().id(5L).product(sampleProduct).quantity(10).build();
        ItemResponse itemResponse = ItemResponse.builder().id(5L).productId(1L).quantity(10).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);
        when(itemMapper.toResponse(savedItem)).thenReturn(itemResponse);

        ItemResponse result = productService.addItemToProduct(1L, request);

        assertThat(result.getQuantity()).isEqualTo(10);
        assertThat(result.getProductId()).isEqualTo(1L);
    }
}
