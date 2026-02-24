package com.zest.productapi.mapper;

import com.zest.productapi.dto.response.ProductResponse;
import com.zest.productapi.entity.Product;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper – generates a Spring bean at compile time.
 * Replaces the manual static {@code ProductResponse.from(Product)} factory.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);
}
