package com.zest.productapi.service.impl;

import com.zest.productapi.dto.request.ItemRequest;
import com.zest.productapi.dto.request.ProductRequest;
import com.zest.productapi.dto.response.ItemResponse;
import com.zest.productapi.dto.response.PagedResponse;
import com.zest.productapi.dto.response.ProductResponse;
import com.zest.productapi.entity.Item;
import com.zest.productapi.entity.Product;
import com.zest.productapi.exception.ResourceNotFoundException;
import com.zest.productapi.util.ItemMapper;
import com.zest.productapi.util.ProductMapper;
import com.zest.productapi.repository.ItemRepository;
import com.zest.productapi.repository.ProductRepository;
import com.zest.productapi.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ItemRepository    itemRepository;
    private final ProductMapper     productMapper;
    private final ItemMapper        itemMapper;

    // ─── Products ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findAll(pageable)
                .map(productMapper::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(String name, Pageable pageable) {
        if (!StringUtils.hasText(name)) {
            return getAllProducts(pageable);
        }
        Page<ProductResponse> page = productRepository
                .findByProductNameContainingIgnoreCase(name, pageable)
                .map(productMapper::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findProductOrThrow(id));
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .productName(request.getProductName())
                .createdBy(currentUsername())
                .createdOn(LocalDateTime.now())
                .build();

        Product saved = productRepository.save(product);
        logProductActivity("CREATED", saved.getId());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductOrThrow(id);
        product.setProductName(request.getProductName());
        product.setModifiedBy(currentUsername());
        product.setModifiedOn(LocalDateTime.now());

        Product saved = productRepository.save(product);
        logProductActivity("UPDATED", saved.getId());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(findProductOrThrow(id));
        logProductActivity("DELETED", id);
    }

    // ─── Items ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ItemResponse> getItemsByProductId(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        Page<ItemResponse> page = itemRepository.findByProductId(productId, pageable)
                .map(itemMapper::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional
    public ItemResponse addItemToProduct(Long productId, ItemRequest request) {
        Product product = findProductOrThrow(productId);
        Item item = Item.builder()
                .product(product)
                .quantity(request.getQuantity())
                .build();
        return itemMapper.toResponse(itemRepository.save(item));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** Async audit log – runs in background thread pool, never blocks the response. */
    @Async("taskExecutor")
    public void logProductActivity(String action, Long productId) {
        log.info("[AUDIT] action={} productId={} by={} at={}",
                action, productId, safeCurrentUsername(), LocalDateTime.now());
    }

    private String safeCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}
