package com.zest.productapi.repository;

import com.zest.productapi.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByProductId(Long productId);

    Page<Item> findByProductId(Long productId, Pageable pageable);
}
