package com.perfume.shop.repository;

import com.perfume.shop.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);

    Optional<ProductVariant> findByProductIdAndSize(Long productId, Integer size);

    void deleteByProductId(Long productId);

    boolean existsByProductIdAndSize(Long productId, Integer size);
}
