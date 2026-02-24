package com.zest.productapi.controller;

import com.zest.productapi.dto.request.ItemRequest;
import com.zest.productapi.dto.request.ProductRequest;
import com.zest.productapi.dto.response.ApiResponse;
import com.zest.productapi.dto.response.ItemResponse;
import com.zest.productapi.dto.response.PagedResponse;
import com.zest.productapi.dto.response.ProductResponse;
import com.zest.productapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Full CRUD operations for Products and their Items")
public class ProductController {

    private final ProductService productService;

    // ─── GET /api/v1/products ────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all products (paginated, optional name search)")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Filter by product name (partial match)")
            @RequestParam(required = false) String name,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<ProductResponse> response = (name != null && !name.isBlank())
                ? productService.searchProducts(name, pageable)
                : productService.getAllProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── GET /api/v1/products/{id} ───────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get a single product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    // ─── POST /api/v1/products ───────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", created));
    }

    // ─── PUT /api/v1/products/{id} ───────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Product updated", productService.updateProduct(id, request)));
    }

    // ─── DELETE /api/v1/products/{id} ────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    // ─── GET /api/v1/products/{id}/items ─────────────────────────────────────

    @GetMapping("/{id}/items")
    @Operation(summary = "List items belonging to a product (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<ItemResponse>>> getItemsByProduct(
            @PathVariable Long id,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(
                ApiResponse.success(productService.getItemsByProductId(id, pageable)));
    }

    // ─── POST /api/v1/products/{id}/items ────────────────────────────────────

    @PostMapping("/{id}/items")
    @Operation(summary = "Add an item to a product")
    public ResponseEntity<ApiResponse<ItemResponse>> addItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {

        ItemResponse item = productService.addItemToProduct(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added", item));
    }
}
