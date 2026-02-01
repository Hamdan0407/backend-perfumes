package com.perfume.shop.service;

import com.perfume.shop.dto.ProductFilterRequest;
import com.perfume.shop.dto.ProductRequest;
import com.perfume.shop.dto.ProductResponse;
import com.perfume.shop.entity.Product;
import com.perfume.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    // ==================== Public Product Queries ====================

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable)
                .map(ProductResponse::fromEntity);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (!product.getActive()) {
            throw new RuntimeException("Product is not available");
        }

        return ProductResponse.fromEntity(product);
    }

    public Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        return productRepository.findByCategoryAndActiveTrue(category, pageable)
                .map(ProductResponse::fromEntity);
    }

    public Page<ProductResponse> getProductsByBrand(String brand, Pageable pageable) {
        return productRepository.findByBrandAndActiveTrue(brand, pageable)
                .map(ProductResponse::fromEntity);
    }

    public List<ProductResponse> getFeaturedProducts() {
        log.info("Fetching featured products - querying database");
        try {
            List<Product> products = productRepository.findByFeaturedTrueAndActiveTrue();
            log.info("Found {} featured products", products.size());

            List<ProductResponse> responses = products.stream()
                    .map(product -> {
                        try {
                            return ProductResponse.fromEntity(product);
                        } catch (Exception e) {
                            log.error("Error converting product ID {} - {}", product.getId(), e.getMessage(), e);
                            throw new RuntimeException("Error processing product: " + e.getMessage());
                        }
                    })
                    .collect(Collectors.toList());

            log.info("Successfully converted {} products to responses", responses.size());
            return responses;
        } catch (Exception e) {
            log.error("CRITICAL: Failed to fetch featured products", e);
            throw new RuntimeException("Failed to fetch featured products: " + e.getMessage(), e);
        }
    }

    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        return productRepository.searchProducts(query, pageable)
                .map(ProductResponse::fromEntity);
    }

    public Page<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByPriceRange(minPrice, maxPrice, pageable)
                .map(ProductResponse::fromEntity);
    }

    public Page<ProductResponse> filterProducts(ProductFilterRequest filter) {
        Sort sort = filter.getSortDir().equalsIgnoreCase("ASC")
                ? Sort.by(filter.getSortBy()).ascending()
                : Sort.by(filter.getSortBy()).descending();

        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        // If search query exists, use search with filters
        if (filter.getSearchQuery() != null && !filter.getSearchQuery().trim().isEmpty()) {
            return productRepository.searchWithFilters(
                    filter.getSearchQuery(),
                    filter.getCategory(),
                    filter.getMinPrice(),
                    filter.getMaxPrice(),
                    pageable).map(ProductResponse::fromEntity);
        }

        // Otherwise use advanced filtering
        return productRepository.findByFilters(
                filter.getCategory(),
                filter.getBrands(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                filter.getFeatured(),
                filter.getMinRating(),
                filter.getInStock(),
                pageable).map(ProductResponse::fromEntity);
    }

    public List<String> getAllBrands() {
        return productRepository.findDistinctBrandByActiveTrue();
    }

    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }

    /**
     * Get all active products (for chatbot AI context) - limited to avoid token
     * overflow
     */
    public List<Product> getAllProducts() {
        Pageable limit = PageRequest.of(0, 50); // Limit to 50 products for AI context
        return productRepository.findByActiveTrue(limit).getContent();
    }

    /**
     * Find product by exact name (case-insensitive) - for chatbot recommendations
     */
    public Product findProductByName(String name) {
        if (name == null || name.isEmpty())
            return null;
        return productRepository.findByNameIgnoreCaseAndActiveTrue(name).orElse(null);
    }

    /**
     * Find product by name containing keyword - for chatbot fuzzy matching
     */
    public Product findProductByNameContains(String keyword) {
        if (keyword == null || keyword.isEmpty())
            return null;
        return productRepository.findByNameContainsIgnoreCaseAndActiveTrue(keyword).orElse(null);
    }

    // ==================== Admin Product Management ====================

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        validateProductRequest(request);

        Product product = Product.builder()
                .name(request.getName())
                .brand(request.getBrand() != null ? request.getBrand() : "Unknown")
                .description(request.getDescription() != null ? request.getDescription() : "Premium perfume")
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .type(request.getType() != null ? request.getType() : "Eau de Parfum")
                .volume(request.getVolume())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : "")
                .additionalImages(request.getAdditionalImages() != null ? request.getAdditionalImages()
                        : new java.util.ArrayList<>())
                .fragranceNotes(
                        request.getFragranceNotes() != null ? request.getFragranceNotes() : new java.util.ArrayList<>())
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .active(request.getActive() != null ? request.getActive() : true)
                .rating(0.0)
                .reviewCount(0)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: {} (ID: {})", saved.getName(), saved.getId());

        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        validateProductRequest(request);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setBrand(request.getBrand());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setType(request.getType());
        product.setVolume(request.getVolume());
        product.setImageUrl(request.getImageUrl());
        product.setAdditionalImages(request.getAdditionalImages());
        product.setFragranceNotes(request.getFragranceNotes());
        product.setFeatured(request.getFeatured());
        product.setActive(request.getActive());

        Product updated = productRepository.save(product);
        log.info("Product updated: {} (ID: {})", updated.getName(), updated.getId());

        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public ProductResponse partialUpdateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (request.getName() != null)
            product.setName(request.getName());
        if (request.getBrand() != null)
            product.setBrand(request.getBrand());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getPrice() != null)
            product.setPrice(request.getPrice());
        if (request.getDiscountPrice() != null)
            product.setDiscountPrice(request.getDiscountPrice());
        if (request.getStock() != null)
            product.setStock(request.getStock());
        if (request.getCategory() != null)
            product.setCategory(request.getCategory());
        if (request.getType() != null)
            product.setType(request.getType());
        if (request.getVolume() != null)
            product.setVolume(request.getVolume());
        if (request.getImageUrl() != null)
            product.setImageUrl(request.getImageUrl());
        if (request.getAdditionalImages() != null)
            product.setAdditionalImages(request.getAdditionalImages());
        if (request.getFragranceNotes() != null)
            product.setFragranceNotes(request.getFragranceNotes());
        if (request.getFeatured() != null)
            product.setFeatured(request.getFeatured());
        if (request.getActive() != null)
            product.setActive(request.getActive());

        Product updated = productRepository.save(product);
        log.info("Product partially updated: {} (ID: {})", updated.getName(), updated.getId());

        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setActive(false);
        productRepository.save(product);
        log.info("Product soft deleted: {} (ID: {})", product.getName(), id);
    }

    @Transactional
    public void permanentDeleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }

        productRepository.deleteById(id);
        log.warn("Product permanently deleted: ID {}", id);
    }

    @Transactional
    public ProductResponse activateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setActive(true);
        Product updated = productRepository.save(product);
        log.info("Product activated: {} (ID: {})", updated.getName(), id);

        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public ProductResponse deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setActive(false);
        Product updated = productRepository.save(product);
        log.info("Product deactivated: {} (ID: {})", updated.getName(), id);

        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public ProductResponse toggleFeatured(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setFeatured(!product.getFeatured());
        Product updated = productRepository.save(product);
        log.info("Product featured status toggled: {} (ID: {}) - Featured: {}",
                updated.getName(), id, updated.getFeatured());

        return ProductResponse.fromEntity(updated);
    }

    // ==================== Stock Management ====================

    @Transactional
    public ProductResponse updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (quantity < 0) {
            throw new RuntimeException("Stock quantity cannot be negative");
        }

        product.setStock(quantity);
        Product updated = productRepository.save(product);
        log.info("Product stock updated: {} (ID: {}) - New stock: {}",
                updated.getName(), id, quantity);

        return ProductResponse.fromEntity(updated);
    }

    @Transactional
    public ProductResponse adjustStock(Long id, Integer adjustment) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        int newStock = product.getStock() + adjustment;
        if (newStock < 0) {
            throw new RuntimeException("Insufficient stock. Current: " + product.getStock());
        }

        product.setStock(newStock);
        Product updated = productRepository.save(product);
        log.info("Product stock adjusted: {} (ID: {}) - Adjustment: {} - New stock: {}",
                updated.getName(), id, adjustment, newStock);

        return ProductResponse.fromEntity(updated);
    }

    public List<ProductResponse> getLowStockProducts(Integer threshold) {
        return productRepository.findLowStockProducts(threshold)
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==================== Admin Queries ====================

    public Page<ProductResponse> getAllProductsAdmin(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::fromEntity);
    }

    public Page<ProductResponse> getProductsByStatus(Boolean active, Pageable pageable) {
        return productRepository.findByActiveStatus(active, pageable)
                .map(ProductResponse::fromEntity);
    }

    public ProductResponse getProductByIdAdmin(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        return ProductResponse.fromEntity(product);
    }

    // ==================== Rating Management ====================

    @Transactional
    public void updateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        Double avgRating = product.getReviews().stream()
                .mapToInt(review -> review.getRating())
                .average()
                .orElse(0.0);

        product.setRating(avgRating);
        product.setReviewCount(product.getReviews().size());

        productRepository.save(product);
        log.info("Product rating updated: {} (ID: {}) - Rating: {} ({} reviews)",
                product.getName(), productId, avgRating, product.getReviewCount());
    }

    // ==================== Statistics ====================

    public Long getTotalActiveProducts() {
        return productRepository.countActiveProducts();
    }

    public Long getTotalOutOfStockProducts() {
        return productRepository.countOutOfStockProducts();
    }

    // ==================== Validation ====================

    private void validateProductRequest(ProductRequest request) {
        if (request.getDiscountPrice() != null &&
                request.getDiscountPrice().compareTo(request.getPrice()) >= 0) {
            throw new RuntimeException("Discount price must be less than regular price");
        }
    }
}
