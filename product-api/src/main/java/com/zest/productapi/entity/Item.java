package com.zest.productapi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "item",
    indexes = {
        @Index(name = "idx_item_product_id", columnList = "product_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
