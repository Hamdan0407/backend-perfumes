package com.perfume.shop.service;

import com.perfume.shop.dto.ProductVariantRequest;
import com.perfume.shop.dto.ProductVariantResponse;
import com.perfume.shop.entity.Product;
import com.perfume.shop.entity.ProductVariant;
import com.perfume.shop.repository.ProductRepository;
import com.perfume.shop.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    private static final List<Integer> ALLOWED_SIZES = List.of(3, 6, 10, 12);

    @Transactional
    public ProductVariantResponse createVariant(Long productId, ProductVariantRequest request) {
        log.info("Creating variant for product {}: {}ml", productId, request.getSize());

        // Validate size
        if (!ALLOWED_SIZES.contains(request.getSize())) {
            throw new RuntimeException("Invalid size. Allowed sizes: " + ALLOWED_SIZES);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if variant with this size already exists
        if (variantRepository.existsByProductIdAndSize(productId, request.getSize())) {
            throw new RuntimeException("Variant with size " + request.getSize() + "ml already exists");
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .size(request.getSize())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stock(request.getStock())
                .sku(request.getSku())
                .active(true)
                .build();

        ProductVariant saved = variantRepository.save(variant);
        log.info("Variant created: ID {}", saved.getId());

        return ProductVariantResponse.fromEntity(saved);
    }

    public List<ProductVariantResponse> getVariantsByProduct(Long productId) {
        return variantRepository.findByProductIdAndActiveTrue(productId).stream()
                .map(ProductVariantResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductVariantResponse updateVariant(Long variantId, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        variant.setPrice(request.getPrice());
        variant.setDiscountPrice(request.getDiscountPrice());
        variant.setStock(request.getStock());
        variant.setSku(request.getSku());

        ProductVariant updated = variantRepository.save(variant);
        log.info("Variant updated: ID {}", updated.getId());

        return ProductVariantResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        variant.setActive(false);
        variantRepository.save(variant);
        log.info("Variant deactivated: ID {}", variantId);
    }
}
