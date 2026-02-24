package com.zest.productapi.service;

import com.zest.productapi.dto.request.ItemRequest;
import com.zest.productapi.dto.request.ProductRequest;
import com.zest.productapi.dto.response.ItemResponse;
import com.zest.productapi.dto.response.PagedResponse;
import com.zest.productapi.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    PagedResponse<ProductResponse> getAllProducts(Pageable pageable);

    PagedResponse<ProductResponse> searchProducts(String name, Pageable pageable);

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    PagedResponse<ItemResponse> getItemsByProductId(Long productId, Pageable pageable);

    ItemResponse addItemToProduct(Long productId, ItemRequest request);
}
