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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "List all products",
               description = "Returns a paginated list of products. Optionally filter by name.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Filter by product name (partial, case-insensitive)")
            @RequestParam(required = false) String name,

            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size")              @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field")             @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "asc or desc")            @RequestParam(defaultValue = "asc") String direction) {

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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    // ─── POST /api/v1/products ───────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Create a new product",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content  = @Content(
                schema   = @Schema(implementation = ProductRequest.class),
                examples = @ExampleObject(
                    name  = "Sample request",
                    value = """
                            { "productName": "Widget Pro X" }
                            """
                )
            )
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.createProduct(request)));
    }

    // ─── PUT /api/v1/products/{id} ───────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing product",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content  = @Content(
                schema   = @Schema(implementation = ProductRequest.class),
                examples = @ExampleObject(
                    name  = "Sample request",
                    value = """
                            { "productName": "Widget Pro X – Revised" }
                            """
                )
            )
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden – ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();   // 204 No Content – correct REST semantics
    }

    // ─── GET /api/v1/products/{id}/items ─────────────────────────────────────

    @GetMapping("/{id}/items")
    @Operation(summary = "List items belonging to a product (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<ItemResponse>>> getItemsByProduct(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(
                ApiResponse.success(productService.getItemsByProductId(id, pageable)));
    }

    // ─── POST /api/v1/products/{id}/items ────────────────────────────────────

    @PostMapping("/{id}/items")
    @Operation(
        summary = "Add an item to a product",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content  = @Content(
                schema   = @Schema(implementation = ItemRequest.class),
                examples = @ExampleObject(
                    name  = "Sample request",
                    value = """
                            { "quantity": 25 }
                            """
                )
            )
        )
    )
    public ResponseEntity<ApiResponse<ItemResponse>> addItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added", productService.addItemToProduct(id, request)));
    }
}
