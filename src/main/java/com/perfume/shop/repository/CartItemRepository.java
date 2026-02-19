package com.perfume.shop.repository;

import com.perfume.shop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    Optional<CartItem> findByCartIdAndProductIdAndVariantIsNull(Long cartId, Long productId);

    Optional<CartItem> findByCartIdAndProductIdAndVariantId(Long cartId, Long productId, Long variantId);

    void deleteByCartIdAndProductId(Long cartId, Long productId);
}
