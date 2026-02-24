package com.zest.productapi.dto.response;

import com.zest.productapi.entity.Item;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse {

    private Long id;
    private Long productId;
    private Integer quantity;

    public static ItemResponse from(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .quantity(item.getQuantity())
                .build();
    }
}
