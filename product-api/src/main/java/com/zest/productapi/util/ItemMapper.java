package com.zest.productapi.util;

import com.zest.productapi.dto.response.ItemResponse;
import com.zest.productapi.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link Item} → {@link ItemResponse}.
 * The nested {@code product.id} is flattened to {@code productId}.
 */
@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(source = "product.id", target = "productId")
    ItemResponse toResponse(Item item);
}
